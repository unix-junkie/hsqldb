/* Copyright (c) 2001-2007, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb;

import org.hsqldb.lib.StringConverter;
import org.hsqldb.rights.Grantee;

/**
 * Provides Name Management for SQL objects. <p>
 *
 * This class now includes the HsqlName class introduced in 1.7.1 and improves
 * auto-naming with multiple databases in the engine.<p>
 *
 * Methods check user defined names and issue system generated names
 * for SQL objects.<p>
 *
 * This class does not deal with the type of the SQL object for which it
 * is used.<p>
 *
 * Some names beginning with SYS_ are reserved for system generated names.
 * These are defined in isReserveName(String name) and created by the
 * makeAutoName(String type) factory method<p>
 *
 * sysNumber is used to generate system-generated names. It is
 * set to the largest integer encountered in names that use the
 * SYS_xxxxxxx_INTEGER format. As the DDL is processed before any ALTER
 * command, any new system generated name will have a larger integer suffix
 * than all the existing names.
 *
 * @author fredt@users
 * @version 1.9.0
 * @since 1.7.2
 */
public class HsqlNameManager {

    private static HsqlNameManager staticManager = new HsqlNameManager();

    static {
        staticManager.serialNumber = Integer.MIN_VALUE;
    }

    private static HsqlName[] autoColumnNames = new HsqlName[16];

    static {
        for (int i = 0; i < autoColumnNames.length; i++) {
            autoColumnNames[i] = new HsqlName(staticManager, "COL_" + (i + 1),
                                              0);
        }
    }

    private int serialNumber = 1;    // 0 is reserved in lookups
    private int sysNumber    = 0;

    public static HsqlName newHsqlSystemObjectName(String name) {
        return new HsqlName(staticManager, name, 0);
    }

    public static HsqlName newInfoSchemaColumnHsqlName(String name,
            HsqlName table) {

        HsqlName hsqlName = new HsqlName(staticManager, name, false,
                                         SchemaObject.COLUMN);

        hsqlName.schema = SchemaManager.INFORMATION_SCHEMA_HSQLNAME;
        hsqlName.parent = table;

        return hsqlName;
    }

    public static HsqlName newInfoSchemaTableHsqlName(String name) {

        HsqlName hsqlName = new HsqlName(staticManager, name, false,
                                         SchemaObject.TABLE);

        hsqlName.schema = SchemaManager.INFORMATION_SCHEMA_HSQLNAME;

        return hsqlName;
    }

    //
    public HsqlName newHsqlName(String name, boolean isquoted, int type) {
        return new HsqlName(this, name, isquoted, type);
    }

    public HsqlName newHsqlName(HsqlName schema, String name,
                                boolean isquoted, int type) {

        HsqlName hsqlName = new HsqlName(this, name, isquoted, type);

        hsqlName.schema = schema;

        return hsqlName;
    }

    public HsqlName newHsqlName(HsqlName schema, String name,
                                boolean isquoted, int type, HsqlName parent) {

        HsqlName hsqlName = new HsqlName(this, name, isquoted, type);

        hsqlName.schema = schema;
        hsqlName.parent = parent;

        return hsqlName;
    }

    /**
     * Same name string but different objects and serial number
     */
    public HsqlName newSubqueryTableName() {

        HsqlName hsqlName = new HsqlName(this, "SYSTEM_SUBQUERY", false,
                                         SchemaObject.TABLE);

        hsqlName.schema = SchemaManager.SYSTEM_SCHEMA_HSQLNAME;

        return hsqlName;
    }

    /**
     * Auto names are used for autogenerated indexes or anonymous constraints.
     */
    public HsqlName newAutoName(String prefix, HsqlName schema,
                                HsqlName parent, int type) {

        HsqlName name = newAutoName(prefix, (String) null, schema, parent,
                                    type);

        return name;
    }

    /**
     * Column index i is 0 based, returns 1 based numbered column.
     */
    static public HsqlName getAutoColumnName(int i) {

        if (i < autoColumnNames.length) {
            return autoColumnNames[i];
        }

        return new HsqlName(staticManager, "COL_" + (i + 1), 0);
    }

    /**
     * Column index i is 0 based, returns 1 based numbered column.
     */
    static public String getAutoColumnNameString(int i) {

        if (i < autoColumnNames.length) {
            return autoColumnNames[i].name;
        }

        return "COL_" + (i + 1);
    }

    /**
     * Auto names are used for autogenerated indexes or anonymous constraints.
     */
    HsqlName newAutoName(String prefix, String namepart, HsqlName schema,
                         HsqlName parent, int type) {

        StringBuffer sbname = new StringBuffer();

        if (prefix != null) {
            if (prefix.length() != 0) {
                sbname.append("SYS_");
                sbname.append(prefix);
                sbname.append('_');

                if (namepart != null) {
                    sbname.append(namepart);
                    sbname.append('_');
                }

                sbname.append(++sysNumber);
            }
        } else {
            sbname.append(namepart);
        }

        HsqlName name = new HsqlName(this, sbname.toString(), type);

        name.schema = schema;
        name.parent = parent;

        return name;
    }

    void resetNumbering() {
        sysNumber    = 0;
        serialNumber = 0;
    }

    public static class HsqlName {

        HsqlNameManager   manager;
        public String     name;
        boolean           isNameQuoted;
        public String     statementName;
        public HsqlName   schema;
        public HsqlName   parent;
        public Grantee    owner;
        public final int  type;
        private final int hashCode;

        private HsqlName(HsqlNameManager man, int type) {

            manager   = man;
            this.type = type;
            hashCode  = manager.serialNumber++;
        }

        private HsqlName(HsqlNameManager man, String name, boolean isquoted,
                         int type) {

            this(man, type);

            rename(name, isquoted);
        }

        private HsqlName(HsqlNameManager man, String name, int type) {

            this(man, type);

            this.name = this.statementName = name;
        }

        public void rename(HsqlName name) {
            rename(name.name, name.isNameQuoted);
        }

        public void rename(String name, boolean isquoted) {

            this.name          = name;
            this.statementName = name;
            this.isNameQuoted  = isquoted;

            if (isNameQuoted) {
                statementName = StringConverter.toQuotedString(name, '"',
                        true);
            }

            if (name.startsWith("SYS_")) {
                int index = name.lastIndexOf('_') + 1;

                try {
                    int temp = Integer.parseInt(name.substring(index));

                    if (temp > manager.sysNumber) {
                        manager.sysNumber = temp;
                    }
                } catch (NumberFormatException e) {}
            }
        }

        void rename(String prefix, String name, boolean isquoted) {

            StringBuffer sbname = new StringBuffer(prefix);

            sbname.append('_');
            sbname.append(name);
            rename(sbname.toString(), isquoted);
        }

        public void setSchemaIfNull(HsqlName schema) {

            if (this.schema == null) {
                this.schema = schema;
            }
        }

        public boolean equals(Object other) {

            if (other instanceof HsqlName) {
                return hashCode == ((HsqlName) other).hashCode;
            }

            return false;
        }

        /**
         * hash code for this object is its unique serial number.
         */
        public int hashCode() {
            return hashCode;
        }

        /**
         * "SYS_IDX_" is used for auto-indexes on referring FK columns or
         * unique constraints.
         * "SYS_PK_" is for the primary key constraints
         * "SYS_CT_" is for unique and check constraints
         * "SYS_REF_" is for FK constraints in referenced tables
         * "SYS_FK_" is for FK constraints in referencing tables
         *
         */
        static boolean isReservedName(String name) {

            return (name.startsWith("SYS_IDX_") || name.startsWith("SYS_PK_")
                    || name.startsWith("SYS_REF_")
                    || name.startsWith("SYS_CT_")
                    || name.startsWith("SYS_FK_"));
        }

        boolean isReservedName() {
            return isReservedName(name);
        }

        public String toString() {

            return getClass().getName() + super.hashCode()
                   + "[this.hashCode()=" + this.hashCode + ", name=" + name
                   + ", name.hashCode()=" + name.hashCode()
                   + ", isNameQuoted=" + isNameQuoted + "]";
        }

        public int compareTo(Object o) {
            return hashCode - o.hashCode();
        }

        /**
         * Returns true if the identifier consists of all uppercase letters
         * digits and underscore, beginning with a letter and is not in the
         * keyword list.
         */
        static boolean isRegularIdentifier(String name) {

            for (int i = 0, length = name.length(); i < length; i++) {
                int c = name.charAt(i);

                if (c >= 'A' && c <= 'Z') {
                    continue;
                } else if (c == '_' && i > 0) {
                    continue;
                } else if (c >= '0' && c <= '9') {
                    continue;
                }

                return false;
            }

            return !Token.isKeyword(name);
        }
    }
}
