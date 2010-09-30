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


package org.hsqldb;

import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.HsqlList;
import org.hsqldb.store.ValuePool;
import org.hsqldb.types.DateTimeType;
import org.hsqldb.types.IntervalType;
import org.hsqldb.types.Type;
import org.hsqldb.types.Types;

/**
 * Implementation of CAST, CASE, LIMIT and ZONE operations.
 *
 * @author Campbell Boucher-Burnett (boucherb@users dot sourceforge.net)
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.0.0
 * @since 1.9.0
 */
public class ExpressionOp extends Expression {

    static final ExpressionOp limitOneExpression = new ExpressionOp(
        OpTypes.LIMIT,
        new ExpressionValue(ValuePool.INTEGER_0, Type.SQL_INTEGER),
        new ExpressionValue(ValuePool.INTEGER_1, Type.SQL_INTEGER));

    /**
     * Creates a special binary operation expression
     */
    ExpressionOp(int type, Expression left, Expression right) {

        super(type);

        nodes        = new Expression[BINARY];
        nodes[LEFT]  = left;
        nodes[RIGHT] = right;

        switch (opType) {

            case OpTypes.ALTERNATIVE :
            case OpTypes.CASEWHEN :
            case OpTypes.LIMIT :
            case OpTypes.ZONE_MODIFIER :
                return;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "ExpressionOp");
        }
    }

    /**
     * creates a CAST expression
     */
    ExpressionOp(Expression e, Type dataType) {

        super(OpTypes.CAST);

        nodes         = new Expression[UNARY];
        nodes[LEFT]   = e;
        this.dataType = dataType;
        this.alias    = e.alias;
    }

    /**
     * creates a special conversion for time / timestamp comparison
     */
    ExpressionOp(Expression e) {

        super(e.dataType.isDateTimeTypeWithZone() ? OpTypes.CAST
                                                  : OpTypes.ZONE_MODIFIER);

        switch (e.dataType.typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE :
                nodes                = new Expression[UNARY];
                nodes[LEFT] = new ExpressionOp(OpTypes.ZONE_MODIFIER, e, null);
                nodes[LEFT].dataType = e.dataType;
                dataType = DateTimeType.getDateTimeType(Types.SQL_TIME,
                        e.dataType.scale);
                break;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                nodes                = new Expression[UNARY];
                nodes[LEFT] = new ExpressionOp(OpTypes.ZONE_MODIFIER, e, null);
                nodes[LEFT].dataType = e.dataType;
                dataType = DateTimeType.getDateTimeType(Types.SQL_TIMESTAMP,
                        e.dataType.scale);
                break;

            case Types.SQL_TIME :
                nodes                = new Expression[BINARY];
                nodes[LEFT]          = e;
                nodes[LEFT].dataType = e.dataType;
                dataType =
                    DateTimeType.getDateTimeType(Types.SQL_TIME_WITH_TIME_ZONE,
                                                 e.dataType.scale);
                break;

            case Types.SQL_TIMESTAMP :
                nodes                = new Expression[BINARY];
                nodes[LEFT]          = e;
                nodes[LEFT].dataType = e.dataType;
                dataType = DateTimeType.getDateTimeType(
                    Types.SQL_TIMESTAMP_WITH_TIME_ZONE, e.dataType.scale);
                break;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "ExpressionOp");
        }

        this.alias = e.alias;
    }

    public static Expression getCastExpression(Session session, Expression e,
            Type dataType) {

        if (e.getType() == OpTypes.VALUE) {
            Object value = dataType.castToType(session, e.getValue(session),
                                               e.getDataType());

            return new ExpressionValue(value, dataType);
        }

        return new ExpressionOp(e, dataType);
    }

    public String getSQL() {

        StringBuffer sb    = new StringBuffer(64);
        String       left  = getContextSQL(nodes.length > 0 ? nodes[LEFT]
                                                            : null);
        String       right = getContextSQL(nodes.length > 1 ? nodes[RIGHT]
                                                            : null);

        switch (opType) {

            case OpTypes.VALUE :
                if (valueData == null) {
                    return Tokens.T_NULL;
                }

                if (dataType == null) {
                    throw Error.runtimeError(ErrorCode.U_S0500,
                                             "ExpressionOp");
                }

                return dataType.convertToSQLString(valueData);

            case OpTypes.CAST :
                sb.append(' ').append(Tokens.T_CAST).append('(');
                sb.append(left).append(' ').append(Tokens.T_AS).append(' ');
                sb.append(dataType.getTypeDefinition());
                sb.append(')');

                return sb.toString();

            case OpTypes.CASEWHEN :
                sb.append(' ').append(Tokens.T_CASEWHEN).append('(');
                sb.append(left).append(',').append(right).append(')');

                return sb.toString();

            case OpTypes.ALTERNATIVE :
                sb.append(left).append(',').append(right);

                return sb.toString();

            case OpTypes.LIMIT :
                if (left != null) {
                    sb.append(' ').append(Tokens.T_OFFSET).append(' ');
                    sb.append(left).append(' ');
                }

                if (right != null) {
                    sb.append(' ').append(Tokens.T_FETCH).append(' ');
                    sb.append(Tokens.T_FIRST);
                    sb.append(right).append(' ').append(right).append(' ');
                    sb.append(Tokens.T_ROWS).append(' ').append(Tokens.T_ONLY);
                    sb.append(' ');
                }
                break;

            case OpTypes.ZONE_MODIFIER :
                sb.append(left).append(' ').append(Tokens.T_AT).append(' ');

                if (nodes[RIGHT] == null) {
                    sb.append(Tokens.T_LOCAL).append(' ');

                    break;
                }

                sb.append(Tokens.T_TIME).append(' ').append(Tokens.T_ZONE);
                sb.append(' ');

                sb.append(right);
                break;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "ExpressionOp");
        }

        return sb.toString();
    }

    protected String describe(Session session, int blanks) {

        StringBuffer sb = new StringBuffer(64);

        sb.append('\n');

        for (int i = 0; i < blanks; i++) {
            sb.append(' ');
        }

        switch (opType) {

            case OpTypes.VALUE :
                sb.append("VALUE = ").append(valueData);
                sb.append(", TYPE = ").append(dataType.getNameString());

                return sb.toString();

            case OpTypes.TABLE :
                sb.append("VALUELIST ");

                for (int i = 0; i < nodes.length; i++) {
                    sb.append(nodes[i].describe(session, blanks + 1));
                    sb.append(' ');
                }
                break;

            case OpTypes.CAST :
                sb.append("CAST ");
                sb.append(dataType.getTypeDefinition());
                sb.append(' ');
                break;

            case OpTypes.CASEWHEN :
                sb.append("CASEWHEN ");
                break;
        }

        if (getLeftNode() != null) {
            sb.append(" arg_left=[");
            sb.append(nodes[LEFT].describe(session, blanks + 1));
            sb.append(']');
        }

        if (getRightNode() != null) {
            sb.append(" arg_right=[");
            sb.append(nodes[RIGHT].describe(session, blanks + 1));
            sb.append(']');
        }

        return sb.toString();
    }

    public HsqlList resolveColumnReferences(Session session,
            RangeVariable[] rangeVarArray, int rangeCount,
            HsqlList unresolvedSet, boolean acceptsSequences) {

        if (opType == OpTypes.VALUE) {
            return unresolvedSet;
        }

        switch (opType) {

            case OpTypes.CASEWHEN :
                acceptsSequences = false;
                break;
        }

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) {
                continue;
            }

            unresolvedSet = nodes[i].resolveColumnReferences(session,
                    rangeVarArray, rangeCount, unresolvedSet,
                    acceptsSequences);
        }

        return unresolvedSet;
    }

    public void resolveTypes(Session session, Expression parent) {

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].resolveTypes(session, this);
            }
        }

        switch (opType) {

            case OpTypes.VALUE :
                break;

            case OpTypes.CAST : {
                Type type = nodes[LEFT].dataType;

                if (type != null && !dataType.canConvertFrom(type)) {
                    throw Error.error(ErrorCode.X_42561);
                }

                if (nodes[LEFT].opType == OpTypes.VALUE) {
                    Expression replacement = nodes[LEFT];

                    setAsConstantValue(session);

                    replacement.dataType  = dataType;
                    replacement.valueData = valueData;

                    if (parent != null) {
                        parent.replaceNode(this, replacement);
                    }

                    break;
                }

                break;
            }
            case OpTypes.ZONE_MODIFIER :
                if (nodes[LEFT].dataType == null) {
                    throw Error.error(ErrorCode.X_42567);
                }

                if (nodes[RIGHT] != null) {
                    if (nodes[RIGHT].dataType == null) {
                        nodes[RIGHT].dataType =
                            Type.SQL_INTERVAL_HOUR_TO_MINUTE;
                    }

                    if (nodes[RIGHT].dataType.typeCode
                            != Types.SQL_INTERVAL_HOUR_TO_MINUTE) {
                        throw Error.error(ErrorCode.X_42563);
                    }
                }

                switch (nodes[LEFT].dataType.typeCode) {

                    case Types.SQL_TIME :
                        dataType = DateTimeType.getDateTimeType(
                            Types.SQL_TIME_WITH_TIME_ZONE,
                            nodes[LEFT].dataType.scale);
                        break;

                    case Types.SQL_TIMESTAMP :
                        dataType = DateTimeType.getDateTimeType(
                            Types.SQL_TIMESTAMP_WITH_TIME_ZONE,
                            nodes[LEFT].dataType.scale);
                        break;

                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                    case Types.SQL_TIME_WITH_TIME_ZONE :
                        dataType = nodes[LEFT].dataType;
                        break;

                    default :
                        throw Error.error(ErrorCode.X_42563);
                }

                // no constant optimisation as value dependent on session zone
                break;

            case OpTypes.CASEWHEN :

                // We use CASEWHEN as parent type.
                // In the parent, left node is the condition, and right node is
                // the leaf, tagged as type ALTERNATIVE; its left node is
                // case 1 (how to get the value when the condition in
                // the parent evaluates to true), while its right node is case 2
                // (how to get the value when the condition in
                // the parent evaluates to false).
                resolveTypesForCaseWhen(session);
                break;

            case OpTypes.ALTERNATIVE :
                break;

            case OpTypes.LIMIT :
                break;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "ExpressionOp");
        }
    }

    /**
     * For CASE WHEN and its special cases section 9.3 of the SQL standard
     * on type aggregation is implemented.
     */
    void resolveTypesForCaseWhen(Session session) {

        if (dataType != null) {
            return;
        }

        Expression expr = this;

        while (expr.opType == OpTypes.CASEWHEN) {
            if (expr.nodes[LEFT].isUnresolvedParam()) {
                expr.nodes[LEFT].dataType = Type.SQL_BOOLEAN;
            }

            expr = expr.nodes[RIGHT].nodes[RIGHT];
        }

        expr = this;

        while (expr.opType == OpTypes.CASEWHEN) {
            dataType =
                Type.getAggregateType(expr.nodes[RIGHT].nodes[LEFT].dataType,
                                      dataType);
            dataType =
                Type.getAggregateType(expr.nodes[RIGHT].nodes[RIGHT].dataType,
                                      dataType);
            expr = expr.nodes[RIGHT].nodes[RIGHT];
        }

        expr = this;

        while (expr.opType == OpTypes.CASEWHEN) {
            if (expr.nodes[RIGHT].nodes[LEFT].dataType == null) {
                expr.nodes[RIGHT].nodes[LEFT].dataType = dataType;
            }

            if (expr.nodes[RIGHT].nodes[RIGHT].dataType == null) {
                expr.nodes[RIGHT].nodes[RIGHT].dataType = dataType;
            }

            if (expr.nodes[RIGHT].dataType == null) {
                expr.nodes[RIGHT].dataType = dataType;
            }

            expr = expr.nodes[RIGHT].nodes[RIGHT];
        }

        if (dataType == null) {
            throw Error.error(ErrorCode.X_42567);
        }
    }

    public Object getValue(Session session) {

        switch (opType) {

            case OpTypes.VALUE :
                return valueData;

            case OpTypes.SIMPLE_COLUMN : {
                Object value =
                    session.sessionContext.rangeIterators[rangePosition]
                        .getCurrent(columnIndex);

                return value;
            }
            case OpTypes.ORDER_BY :
                return nodes[LEFT].getValue(session);

            case OpTypes.CAST : {
                Object value =
                    dataType.castToType(session,
                                        nodes[LEFT].getValue(session),
                                        nodes[LEFT].dataType);

                if (dataType.userTypeModifier != null) {
                    Constraint[] constraints =
                        dataType.userTypeModifier.getConstraints();

                    for (int i = 0; i < constraints.length; i++) {
                        constraints[i].checkCheckConstraint(session, null,
                                                            value);
                    }
                }

                return value;
            }
            case OpTypes.CASEWHEN : {
                Boolean result = (Boolean) nodes[LEFT].getValue(session);

                if (Boolean.TRUE.equals(result)) {
                    return nodes[RIGHT].nodes[LEFT].getValue(session,
                            dataType);
                } else {
                    return nodes[RIGHT].nodes[RIGHT].getValue(session,
                            dataType);
                }
            }
            case OpTypes.ZONE_MODIFIER : {
                Object leftValue  = nodes[LEFT].getValue(session);
                Object rightValue = nodes[RIGHT] == null ? null
                                                         : nodes[RIGHT]
                                                             .getValue(
                                                                 session);

                if (leftValue == null) {
                    return null;
                }

                if (nodes[RIGHT] != null && rightValue == null) {
                    return null;
                }

                long zoneSeconds = nodes[RIGHT] == null
                                   ? session.getZoneSeconds()
                                   : ((IntervalType) nodes[RIGHT].dataType)
                                       .getSeconds(rightValue);

                return ((DateTimeType) dataType).changeZone(leftValue,
                        nodes[LEFT].dataType, (int) zoneSeconds,
                        session.getZoneSeconds());
            }
            case OpTypes.LIMIT :

            // fall through
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "ExpressionOp");
        }
    }
}
