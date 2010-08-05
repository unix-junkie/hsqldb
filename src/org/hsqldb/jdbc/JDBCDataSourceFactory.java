/* Copyright (c) 2001-2010, The HSQL Development Group
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


package org.hsqldb.jdbc;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

// boucherb@users 20040411 - doc 1.7.2 - javadoc updates toward 1.7.2 final

/**
 * A JNDI ObjectFactory for creating {@link JDBCDataSource JDBCDataSource}
 * object instances.
 *
 * @author deforest@users
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.0.1
 * @version 1.7.2
 */
public class JDBCDataSourceFactory implements ObjectFactory {

    /**
     * Creates a JDBCDataSource, JDBCPooledDataSource or JDBCXADataSource object
     * using the javax.naming.Reference object specified.<p>
     *
     * The Reference object's class name should be one of the three supported
     * data source class names and it should support the properties, database,
     * user and password. It may optionally support the logingTimeout property.
     *
     * @param obj The reference information used in creating a
     *      JDBCDatasource object.
     * @param name ignored
     * @param nameCtx ignored
     * @param environment ignored
     * @return A newly created JDBCDataSource object; null if an object
     *      cannot be created.
     * @exception Exception is thrown if database or user is null or invalid
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment) throws Exception {

        Reference ref = (Reference) obj;

        if (!(ref instanceof Reference)) {
            return null;
        }

        String className = ref.getClassName();

        if (className == null) {
            return null;
        }

        if (className.equals(bdsClass) || className.equals(pdsClass)
                || className.equals(xdsClass)) {
            JDBCCommonDataSource ds =
                (JDBCCommonDataSource) Class.forName(className).newInstance();
            Object value = ref.get("database").getContent();

            if (!(value instanceof String)) {
                throw new Exception(className + ": invalid RefAddr: database");
            }

            ds.setDatabase((String) value);

            value = ref.get("user").getContent();

            if (!(value instanceof String)) {
                throw new Exception(className + ": invalid RefAddr: user");
            }

            ds.setUser((String) value);

            value = ref.get("password").getContent();

            if (!(value instanceof String)) {
                value = "";
            }

            ds.setPassword((String) value);

            String loginTimeoutContent =
                (String) ref.get("loginTimeout").getContent();

            if (loginTimeoutContent != null) {
                loginTimeoutContent = loginTimeoutContent.trim();

                if (loginTimeoutContent.length() > 0) {
                    try {
                        ds.setLoginTimeout(
                            Integer.parseInt(loginTimeoutContent));
                    } catch (NumberFormatException nfe) {}
                }
            }

            return ds;
        } else {
            return null;
        }
    }

    private final static String bdsClass = "org.hsqldb.jdbc.JDBCDataSource";
    private final static String pdsClass =
        "org.hsqldb.jdbc.pool.JDBCPooledDataSource";
    private final static String xdsClass =
        "org.hsqldb.jdbc.pool.JDBCXADataSource";

    public JDBCDataSourceFactory() {}
}
