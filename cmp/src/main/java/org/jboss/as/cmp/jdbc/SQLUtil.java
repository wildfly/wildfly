/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cmp.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import java.util.zip.CRC32;
import javax.sql.DataSource;

import org.jboss.as.cmp.CmpLogger;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.logging.Logger;

/**
 * SQLUtil helps with building sql statements.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @author <a href="joachim@cabsoft.be">Joachim Van der Auwera</a>
 * @version $Revision: 81030 $
 */
public final class SQLUtil {
    public static final String EMPTY_STRING = "";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String VALUES = " VALUES ";
    public static final String SELECT = "SELECT ";
    public static final String DISTINCT = "DISTINCT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String ORDERBY = " ORDER BY ";
    public static final String DELETE_FROM = "DELETE FROM ";
    public static final String AND = " AND ";
    public static final String OR = " OR ";
    public static final String NOT = " NOT ";
    public static final String EXISTS = "EXISTS ";
    public static final String COMMA = ", ";
    public static final String LEFT_JOIN = " LEFT JOIN ";
    public static final String LEFT_OUTER_JOIN = " LEFT OUTER JOIN ";
    public static final String ON = " ON ";
    public static final String NOT_EQUAL = "<>";
    public static final String CREATE_TABLE = "CREATE TABLE ";
    public static final String DROP_TABLE = "DROP TABLE ";
    public static final String CREATE_INDEX = "CREATE INDEX ";
    public static final String NULL = "NULL";
    public static final String IS = " IS ";
    public static final String IN = " IN ";
    public static final String EMPTY = "EMPTY";
    public static final String BETWEEN = " BETWEEN ";
    public static final String LIKE = " LIKE ";
    public static final String MEMBER_OF = " MEMBER OF ";
    public static final String CONCAT = "CONCAT";
    public static final String SUBSTRING = "SUBSTRING";
    public static final String LCASE = "LCASE";
    public static final String UCASE = "UCASE";
    public static final String LENGTH = "LENGTH";
    public static final String LOCATE = "LOCATE";
    public static final String ABS = "ABS";
    public static final String MOD = "MOD";
    public static final String SQRT = "SQRT";
    public static final String COUNT = "COUNT";
    public static final String MAX = "MAX";
    public static final String MIN = "MIN";
    public static final String AVG = "AVG";
    public static final String SUM = "SUM";
    public static final String ASC = " ASC";
    public static final String DESC = " DESC";
    public static final String OFFSET = " OFFSET ";
    public static final String LIMIT = " LIMIT ";
    public static final String UPDATE = "UPDATE ";
    public static final String SET = " SET ";
    public static final String TYPE = " TYPE ";
    private static final String DOT = ".";

    private static final String EQ_QUESTMARK = "=?";

    private static final Vector rwords = new Vector();

    public static String getTableNameWithoutSchema(String tableName) {
        final int dot = tableName.indexOf('.');
        if (dot != -1) {
            char firstChar = tableName.charAt(0);
            tableName = tableName.substring(dot + 1);
            if (firstChar == '"' || firstChar == '\'') {
                tableName = firstChar + tableName;
            }
        }
        return tableName;
    }

    public static String getSchema(String tableName) {
        String schema = null;
        final int dot = tableName.indexOf('.');
        if (dot != -1) {
            char firstChar = tableName.charAt(0);
            final boolean quoted = firstChar == '"' || firstChar == '\'';
            schema = tableName.substring(quoted ? 1 : 0, dot);
        }
        return schema;
    }

    public static String fixTableName(String tableName, DataSource dataSource) {
        // don't fix the quited table name
        char firstChar = tableName.charAt(0);
        if (firstChar == '"' || firstChar == '\'') {
            return tableName;
        }

        // Separate schema name and table name
        String strSchema = "";
        int iIndex;
        if ((iIndex = tableName.indexOf('.')) != -1) {
            strSchema = tableName.substring(0, iIndex);
            tableName = tableName.substring(iIndex + 1);
        }

        // check for SQL reserved word and escape it with prepending a "X"
        // IMHO one should reject reserved words and throw a
        if (rwords != null) {
            for (int i = 0; i < rwords.size(); i++) {
                if (((String) rwords.elementAt(i)).equalsIgnoreCase(tableName)) {
                    tableName = "X" + tableName;
                    break;
                }
            }
        }

        Connection con = null;
        try {
            con = dataSource.getConnection();
            DatabaseMetaData dmd = con.getMetaData();

            // fix length
            int maxLength = dmd.getMaxTableNameLength();
            if (maxLength > 0 && tableName.length() > maxLength) {
                CRC32 crc = new CRC32();
                crc.update(tableName.getBytes());
                String nameCRC = Long.toString(crc.getValue(), 36);

                tableName = tableName.substring(
                        0,
                        maxLength - nameCRC.length() - 2);
                tableName += "_" + nameCRC;
            }

            // fix case
            if (dmd.storesLowerCaseIdentifiers()) {
                tableName = tableName.toLowerCase();
            } else if (dmd.storesUpperCaseIdentifiers()) {
                tableName = tableName.toUpperCase();
            }
            // now put the schema name back on the table name
            if (strSchema.length() > 0) {
                tableName = strSchema + "." + tableName;
            }
            return tableName;
        } catch (SQLException e) {
            // This should not happen. A J2EE compatiable JDBC driver is
            // required fully support metadata.
            throw MESSAGES.errorFixingTableName(e);
        } finally {
            JDBCUtil.safeClose(con);
        }
    }

    public static void addToRwords(String word) {
        if (!rwords.contains(word))
            rwords.add(word);
    }


    public static String fixConstraintName(String name, DataSource dataSource) {
        return fixTableName(name, dataSource).replace('.', '_');
    }

    // =======================================================================
    //  Create Table Columns Clause
    //    columnName0 sqlType0
    //    [, columnName1 sqlType0
    //    [, columnName2 sqlType0 [...]]]
    // =======================================================================
    public static String getCreateTableColumnsClause(JDBCFieldBridge[] fields) {
        StringBuffer buf = new StringBuffer(100);
        boolean comma = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (comma)
                    buf.append(COMMA);
                else
                    comma = true;
                buf.append(getCreateTableColumnsClause(type));
            }
        }
        return buf.toString();
    }

    /**
     * Returns columnName0 sqlType0
     * [, columnName1 sqlType0
     * [, columnName2 sqlType0 [...]]]
     */
    public static String getCreateTableColumnsClause(JDBCType type) {
        String[] columnNames = type.getColumnNames();
        String[] sqlTypes = type.getSQLTypes();
        boolean[] notNull = type.getNotNull();

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < columnNames.length; i++) {
            if (i != 0)
                buf.append(COMMA);
            buf.append(columnNames[i]).append(' ').append(sqlTypes[i]);
            if (notNull[i])
                buf.append(NOT).append(NULL);
        }
        return buf.toString();
    }

    // =======================================================================
    //  Column Names Clause
    //    columnName0 [, columnName1 [AND columnName2 [...]]]
    // =======================================================================

    /**
     * Returns columnName0 [, columnName1 [AND columnName2 [...]]]
     */
    public static StringBuffer getColumnNamesClause(JDBCFieldBridge[] fields, StringBuffer sb) {
        return getColumnNamesClause(fields, "", sb);
    }

    /**
     * Returns columnName0 [, columnName1 [AND columnName2 [...]]]
     */
    public static StringBuffer getColumnNamesClause(JDBCFieldBridge[] fields,
                                                    String identifier,
                                                    StringBuffer buf) {
        boolean comma = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (comma)
                    buf.append(COMMA);
                else
                    comma = true;
                getColumnNamesClause(type, identifier, buf);
            }
        }
        return buf;
    }

    /**
     * Returns columnName0 [, columnName1 [AND columnName2 [...]]]
     */
    public static StringBuffer getSearchableColumnNamesClause(JDBCFieldBridge[] fields,
                                                              String identifier,
                                                              StringBuffer buf) {
        boolean comma = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null && type.isSearchable()) {
                if (comma)
                    buf.append(COMMA);
                else
                    comma = true;
                getColumnNamesClause(type, identifier, buf);
            }
        }
        return buf;
    }

    /**
     * Returns columnName0 [, columnName1 [AND columnName2 [...]]]
     */
    public static StringBuffer getColumnNamesClause(JDBCEntityBridge.FieldIterator loadIter, StringBuffer sb) {
        if (loadIter.hasNext())
            getColumnNamesClause(loadIter.next(), sb);
        while (loadIter.hasNext()) {
            sb.append(COMMA);
            getColumnNamesClause(loadIter.next(), sb);
        }
        return sb;
    }

    /**
     * Returns columnName0 [, columnName1 [, columnName2 [...]]]
     */
    public static StringBuffer getColumnNamesClause(JDBCFieldBridge field, StringBuffer sb) {
        return getColumnNamesClause(field.getJDBCType(), sb);
    }

    /**
     * Returns identifier.columnName0
     * [, identifier.columnName1
     * [, identifier.columnName2 [...]]]
     */
    public static StringBuffer getColumnNamesClause(JDBCFieldBridge field, String identifier, StringBuffer sb) {
        return getColumnNamesClause(field.getJDBCType(), identifier, sb);
    }

    /**
     * Returns identifier.columnName0
     * [, identifier.columnName1
     * [, identifier.columnName2 [...]]]
     */
    private static StringBuffer getColumnNamesClause(JDBCType type, String identifier, StringBuffer buf) {
        String[] columnNames = type.getColumnNames();
        boolean hasIdentifier = identifier.length() > 0;
        if (hasIdentifier)
            buf.append(identifier).append(DOT);
        buf.append(columnNames[0]);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(COMMA);
            if (hasIdentifier)
                buf.append(identifier).append(DOT);
            buf.append(columnNames[i++]);
        }
        return buf;
    }

    /**
     * Returns ', columnName0 [, columnName1 [AND columnName2 [...]]]'
     */
    public static StringBuffer appendColumnNamesClause(JDBCAbstractEntityBridge entity, String eagerLoadGroup, StringBuffer sb) {
        return appendColumnNamesClause(entity, eagerLoadGroup, "", sb);
    }

    /**
     * Returns ', columnName0 [, columnName1 [AND columnName2 [...]]]'
     */
    public static StringBuffer appendColumnNamesClause(JDBCAbstractEntityBridge entity,
                                                       String eagerLoadGroup,
                                                       String alias,
                                                       StringBuffer sb) {
        return appendColumnNamesClause(entity.getTableFields(), entity.getLoadGroupMask(eagerLoadGroup), alias, sb);
    }

    /**
     * Returns ', columnName0 [, columnName1 [AND columnName2 [...]]]'
     */
    public static StringBuffer appendColumnNamesClause(JDBCFieldBridge[] fields,
                                                       boolean[] mask,
                                                       String identifier,
                                                       StringBuffer buf) {
        for (int i = 0; i < fields.length; ++i) {
            if (mask[i]) {
                JDBCType type = getJDBCType(fields[i]);
                if (type != null) {
                    buf.append(COMMA);
                    getColumnNamesClause(type, identifier, buf);
                }
            }
        }
        return buf;
    }

    public static StringBuffer appendSearchableColumnNamesClause(JDBCFieldBridge[] fields,
                                                                 boolean[] mask,
                                                                 String identifier,
                                                                 StringBuffer buf) {
        for (int i = 0; i < fields.length; ++i) {
            if (mask[i]) {
                JDBCType type = getJDBCType(fields[i]);
                if (type != null && type.isSearchable()) {
                    buf.append(COMMA);
                    getColumnNamesClause(type, identifier, buf);
                }
            }
        }
        return buf;
    }

    /**
     * Returns ', columnName0 [, columnName1 [AND columnName2 [...]]]'
     */
    public static StringBuffer appendColumnNamesClause(JDBCFieldBridge[] fields,
                                                       String identifier,
                                                       StringBuffer buf) {
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                buf.append(COMMA);
                getColumnNamesClause(type, identifier, buf);
            }
        }
        return buf;
    }

    /**
     * Returns identifier.columnName0
     * [, identifier.columnName1
     * [, identifier.columnName2 [...]]]
     */
    private static StringBuffer getColumnNamesClause(JDBCType type, StringBuffer buf) {
        String[] columnNames = type.getColumnNames();
        buf.append(columnNames[0]);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(COMMA).append(columnNames[i++]);
        }
        return buf;
    }

    // =======================================================================
    //  Set Clause
    //    columnName0=? [, columnName1=? [, columnName2=? [...]]]
    // =======================================================================

    /**
     * Returns columnName0=? [, columnName1=? [, columnName2=? [...]]]
     */
    public static StringBuffer getSetClause(JDBCEntityBridge.FieldIterator fieldsIter,
                                            StringBuffer buf) {
        JDBCType type = getJDBCType(fieldsIter.next());
        getSetClause(type, buf);
        while (fieldsIter.hasNext()) {
            type = getJDBCType(fieldsIter.next());
            buf.append(COMMA);
            getSetClause(type, buf);
        }
        return buf;
    }

    /**
     * Returns columnName0=? [, columnName1=? [, columnName2=? [...]]]
     */
    private static StringBuffer getSetClause(JDBCType type, StringBuffer buf) {
        String[] columnNames = type.getColumnNames();
        buf.append(columnNames[0]).append(EQ_QUESTMARK);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(COMMA).append(columnNames[i++]).append(EQ_QUESTMARK);
        }
        return buf;
    }

    // =======================================================================
    //  Values Clause
    //    ? [, ? [, ? [...]]]
    // =======================================================================

    /**
     * Returns ? [, ? [, ? [...]]]
     */
    public static StringBuffer getValuesClause(JDBCFieldBridge[] fields, StringBuffer buf) {
        boolean comma = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (comma)
                    buf.append(COMMA);
                else
                    comma = true;
                getValuesClause(type, buf);
            }
        }
        return buf;
    }

    /**
     * Returns ? [, ? [, ? [...]]]
     */
    private static StringBuffer getValuesClause(JDBCType type, StringBuffer buf) {
        int columnCount = type.getColumnNames().length;
        buf.append('?');
        int i = 1;
        while (i++ < columnCount)
            buf.append(COMMA).append('?');
        return buf;
    }
//
    // =======================================================================
    //  Where Clause
    //    columnName0=? [AND columnName1=? [AND columnName2=? [...]]]
    // =======================================================================

    /**
     * Returns columnName0=? [AND columnName1=? [AND columnName2=? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCFieldBridge[] fields, StringBuffer buf) {
        return getWhereClause(fields, "", buf);
    }

    /**
     * Returns identifier.columnName0=?
     * [AND identifier.columnName1=?
     * [AND identifier.columnName2=? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCFieldBridge[] fields, String identifier, StringBuffer buf) {
        boolean and = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (and)
                    buf.append(AND);
                else
                    and = true;
                getWhereClause(type, identifier, buf);
            }
        }
        return buf;
    }

    /**
     * Returns columnName0=? [AND columnName1=? [AND columnName2=? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCFieldBridge[] fields,
                                              long mask,
                                              StringBuffer buf) {
        return getWhereClause(fields, mask, "", buf);
    }

    /**
     * Returns columnName0=? [AND columnName1=? [AND columnName2=? [...]]]
     */
    private static StringBuffer getWhereClause(JDBCFieldBridge[] fields,
                                               long mask,
                                               String identifier,
                                               StringBuffer buf) {
        boolean and = false;
        long fieldMask = 1;
        for (int i = 0; i < fields.length; ++i) {
            if ((fieldMask & mask) > 0) {
                JDBCType type = getJDBCType(fields[i]);
                if (type != null) {
                    if (and)
                        buf.append(AND);
                    else
                        and = true;
                    getWhereClause(type, identifier, buf);
                }
            }
            fieldMask <<= 1;
        }
        return buf;
    }

    /**
     * Returns columnName0=? [AND columnName1=? [AND columnName2=? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCFieldBridge field, StringBuffer buf) {
        return getWhereClause(field.getJDBCType(), "", buf);
    }

    /**
     * Returns identifier.columnName0=?
     * [AND identifier.columnName1=?
     * [AND identifier.columnName2=? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCType type, String identifier, StringBuffer buf) {
        if (identifier.length() > 0) {
            identifier += '.';
        }

        String[] columnNames = type.getColumnNames();
        buf.append(identifier).append(columnNames[0]).append(EQ_QUESTMARK);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(AND).append(identifier).append(columnNames[i++]).append(EQ_QUESTMARK);
        }
        return buf;
    }

    /**
     * Returns identifier.columnName0{comparison}?
     * [AND identifier.columnName1{comparison}?
     * [AND identifier.columnName2{comparison}? [...]]]
     */
    public static StringBuffer getWhereClause(JDBCType type, String identifier, String comparison, StringBuffer buf) {
        if (identifier.length() > 0) {
            identifier += '.';
        }

        String[] columnNames = type.getColumnNames();
        buf.append(identifier).append(columnNames[0]).append(comparison).append('?');
        int i = 1;
        while (i < columnNames.length) {
            buf.append(AND).append(identifier).append(columnNames[i++]).append(comparison).append('?');
        }
        return buf;
    }


    // =======================================================================
    //  Is [Not] Null Clause
    //    columnName0 IS [NOT] NULL [AND columnName1 IS [NOT] NULL [...]]
    // =======================================================================

    /**
     * Returns identifier.columnName0 IS [NOT] NULL
     * [AND identifier.columnName1 IS [NOT] NULL
     * [AND identifier.columnName2 IS [NOT] NULL [...]]]
     */
    public static StringBuffer getIsNullClause(boolean not,
                                               JDBCFieldBridge[] fields,
                                               String identifier,
                                               StringBuffer buf) {
        boolean and = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (and)
                    buf.append(AND);
                else
                    and = true;
                getIsNullClause(not, type, identifier, buf);
            }
        }
        return buf;
    }

    /**
     * Returns identifier.columnName0 IS [NOT] NULL
     * [AND identifier.columnName1 IS [NOT] NULL
     * [AND identifier.columnName2 IS [NOT] NULL [...]]]
     */
    public static StringBuffer getIsNullClause(boolean not,
                                               JDBCFieldBridge field,
                                               String identifier,
                                               StringBuffer buf) {
        return getIsNullClause(not, field.getJDBCType(), identifier, buf);
    }

    /**
     * Returns identifier.columnName0 IS [NOT] NULL
     * [AND identifier.columnName1 IS [NOT] NULL
     * [AND identifier.columnName2 IS [NOT] NULL [...]]]
     */
    private static StringBuffer getIsNullClause(boolean not,
                                                JDBCType type,
                                                String identifier,
                                                StringBuffer buf) {
        if (identifier.length() > 0) {
            identifier += '.';
        }

        String[] columnNames = type.getColumnNames();

        buf.append(identifier).append(columnNames[0]).append(IS);
        (not ? buf.append(NOT) : buf).append(NULL);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(AND).append(identifier).append(columnNames[i++]).append(IS);
            (not ? buf.append(NOT) : buf).append(NULL);
        }
        return buf;
    }

    // =======================================================================
    //  Join Clause
    //    parent.pkColumnName0=child.fkColumnName0
    //    [AND parent.pkColumnName1=child.fkColumnName1
    //    [AND parent.pkColumnName2=child.fkColumnName2 [...]]]
    // =======================================================================

    public static StringBuffer getJoinClause(JDBCAbstractCMRFieldBridge cmrField,
                                             String parentAlias,
                                             String childAlias,
                                             StringBuffer buf) {
        JDBCAbstractEntityBridge parentEntity = cmrField.getEntity();
        JDBCAbstractEntityBridge childEntity = (JDBCAbstractEntityBridge) cmrField.getRelatedEntity();

        JDBCFieldBridge parentField;
        JDBCFieldBridge childField;

        if (cmrField.hasForeignKey()) {
            // parent has the foreign keys
            JDBCFieldBridge[] parentFkFields = cmrField.getForeignKeyFields();
            int i = 0;
            while (i < parentFkFields.length) {
                parentField = parentFkFields[i++];
                childField = (JDBCFieldBridge) childEntity.getFieldByName(parentField.getFieldName());
                getJoinClause(parentField, parentAlias, childField, childAlias, buf);
                if (i < parentFkFields.length)
                    buf.append(AND);
            }
        } else {
            // child has the foreign keys
            JDBCFieldBridge[] childFkFields = cmrField.getRelatedCMRField().getForeignKeyFields();
            int i = 0;
            while (i < childFkFields.length) {
                childField = childFkFields[i++];
                parentField = (JDBCFieldBridge) parentEntity.getFieldByName(childField.getFieldName());

                // add the sql
                getJoinClause(parentField, parentAlias, childField, childAlias, buf);
                if (i < childFkFields.length) {
                    buf.append(AND);
                }
            }
        }
        return buf;
    }

    public static StringBuffer getRelationTableJoinClause(JDBCAbstractCMRFieldBridge cmrField,
                                                          String parentAlias,
                                                          String relationTableAlias,
                                                          StringBuffer buf) {
        JDBCAbstractEntityBridge parentEntity = cmrField.getEntity();
        JDBCFieldBridge parentField;
        JDBCFieldBridge relationField;

        // parent to relation table join
        JDBCFieldBridge[] parentFields = cmrField.getTableKeyFields();
        int i = 0;
        while (i < parentFields.length) {
            relationField = parentFields[i++];
            parentField = (JDBCFieldBridge) parentEntity.getFieldByName(relationField.getFieldName());
            getJoinClause(parentField, parentAlias, relationField, relationTableAlias, buf);
            if (i < parentFields.length)
                buf.append(AND);
        }
        return buf;
    }

    /**
     * Returns parent.pkColumnName0=child.fkColumnName0
     * [AND parent.pkColumnName1=child.fkColumnName1
     * [AND parent.pkColumnName2=child.fkColumnName2 [...]]]
     */
    private static StringBuffer getJoinClause(JDBCFieldBridge pkField,
                                              String parent,
                                              JDBCFieldBridge fkField,
                                              String child,
                                              StringBuffer buf) {
        return getJoinClause(pkField.getJDBCType(), parent, fkField.getJDBCType(), child, buf);
    }

    public static StringBuffer getJoinClause(JDBCFieldBridge[] pkFields,
                                             String parent,
                                             JDBCFieldBridge[] fkFields,
                                             String child,
                                             StringBuffer buf) {
        if (pkFields.length != fkFields.length) {
            throw CmpMessages.MESSAGES.errorCreatingJoin(pkFields.length, fkFields.length);
        }

        boolean and = false;
        for (int i = 0; i < pkFields.length; ++i) {
            // these types should not be null
            JDBCType pkType = getJDBCType(pkFields[i]);
            JDBCType fkType = getJDBCType(fkFields[i]);
            if (and)
                buf.append(AND);
            else
                and = true;
            getJoinClause(pkType, parent, fkType, child, buf);
        }
        return buf;
    }

    /**
     * Returns parent.pkColumnName0=child.fkColumnName0
     * [AND parent.pkColumnName1=child.fkColumnName1
     * [AND parent.pkColumnName2=child.fkColumnName2 [...]]]
     */
    private static StringBuffer getJoinClause(JDBCType pkType,
                                              String parent,
                                              JDBCType fkType,
                                              String child,
                                              StringBuffer buf) {
        if (parent.length() > 0) {
            parent += '.';
        }
        if (child.length() > 0) {
            child += '.';
        }

        String[] pkColumnNames = pkType.getColumnNames();
        String[] fkColumnNames = fkType.getColumnNames();
        if (pkColumnNames.length != fkColumnNames.length) {
            throw MESSAGES.pkAndFkWrongNumberOfColumns();
        }

        buf.append(parent).append(pkColumnNames[0]).append('=').append(child).append(fkColumnNames[0]);
        int i = 1;
        while (i < pkColumnNames.length) {
            buf.append(AND)
                    .append(parent)
                    .append(pkColumnNames[i])
                    .append('=')
                    .append(child)
                    .append(fkColumnNames[i++]);
        }
        return buf;
    }

    // =======================================================================
    //  Self Compare Where Clause
    //    fromIdentifier.pkColumnName0=toIdentifier.fkColumnName0
    //    [AND fromIdentifier.pkColumnName1=toIdentifier.fkColumnName1
    //    [AND fromIdentifier.pkColumnName2=toIdentifier.fkColumnName2 [...]]]
    // =======================================================================

    public static StringBuffer getSelfCompareWhereClause(JDBCFieldBridge[] fields,
                                                         String fromIdentifier,
                                                         String toIdentifier,
                                                         StringBuffer buf) {
        boolean and = false;
        for (int i = 0; i < fields.length; ++i) {
            JDBCType type = getJDBCType(fields[i]);
            if (type != null) {
                if (and)
                    buf.append(AND);
                else
                    and = true;
                getSelfCompareWhereClause(type, fromIdentifier, toIdentifier, buf);
            }
        }
        return buf;
    }

    private static StringBuffer getSelfCompareWhereClause(JDBCType type,
                                                          String fromIdentifier,
                                                          String toIdentifier,
                                                          StringBuffer buf) {
        if (fromIdentifier.length() > 0)
            fromIdentifier += '.';
        if (toIdentifier.length() > 0)
            toIdentifier += '.';

        String[] columnNames = type.getColumnNames();

        buf.append(fromIdentifier)
                .append(columnNames[0])
                .append('=')
                .append(toIdentifier)
                .append(columnNames[0]);
        int i = 1;
        while (i < columnNames.length) {
            buf.append(AND)
                    .append(fromIdentifier)
                    .append(columnNames[i])
                    .append('=')
                    .append(toIdentifier)
                    .append(columnNames[i++]);
        }
        return buf;
    }

    public static StringBuffer getSelfCompareWhereClause(JDBCFieldBridge fromField,
                                                         JDBCFieldBridge toField,
                                                         String fromIdentifier,
                                                         String toIdentifier,
                                                         String comparison,
                                                         StringBuffer buf) {
        return getSelfCompareWhereClause(
                fromField.getJDBCType(), toField.getJDBCType(), fromIdentifier, toIdentifier, comparison, buf
        );
    }

    private static StringBuffer getSelfCompareWhereClause(JDBCType fromType,
                                                          JDBCType toType,
                                                          String fromIdentifier,
                                                          String toIdentifier,
                                                          String comparison,
                                                          StringBuffer buf) {
        if (fromIdentifier.length() > 0)
            fromIdentifier += '.';
        if (toIdentifier.length() > 0)
            toIdentifier += '.';

        String[] fromColumnNames = fromType.getColumnNames();
        String[] toColumnNames = toType.getColumnNames();

        buf.append(fromIdentifier)
                .append(fromColumnNames[0])
                .append(comparison)
                .append(toIdentifier)
                .append(toColumnNames[0]);
        int i = 1;
        while (i < fromColumnNames.length) {
            buf.append(AND)
                    .append(fromIdentifier)
                    .append(fromColumnNames[i])
                    .append(comparison)
                    .append(toIdentifier)
                    .append(toColumnNames[i++]);
        }
        return buf;
    }

    public static boolean tableExists(String tableName, DataSource dataSource) {
        Connection con = null;
        ResultSet rs = null;
        try {
            con = dataSource.getConnection();

            // (a j2ee spec compatible jdbc driver has to fully
            // implement the DatabaseMetaData)
            DatabaseMetaData dmd = con.getMetaData();
            String catalog = con.getCatalog();
            String schema = null;
            String quote = dmd.getIdentifierQuoteString();
            if (tableName.startsWith(quote)) {
                if (tableName.endsWith(quote) == false) {
                    throw MESSAGES.mismatchedQuoteTableName(tableName);
                }
                int quoteLength = quote.length();
                tableName = tableName.substring(quoteLength, tableName.length() - quoteLength);
                if (dmd.storesLowerCaseQuotedIdentifiers())
                    tableName = tableName.toLowerCase();
                else if (dmd.storesUpperCaseQuotedIdentifiers())
                    tableName = tableName.toUpperCase();
            } else {
                if (dmd.storesLowerCaseIdentifiers())
                    tableName = tableName.toLowerCase();
                else if (dmd.storesUpperCaseIdentifiers())
                    tableName = tableName.toUpperCase();
            }

            // Patch #927759: Split tablename into "schema" and "table" separated by '.'
            int dotIndex;
            if ((dotIndex = tableName.indexOf('.')) != -1) {
                // Yank out schema name ...
                schema = tableName.substring(0, dotIndex);
                tableName = tableName.substring(dotIndex + 1);
            }

            rs = dmd.getTables(catalog, schema, tableName, null);
            return rs.next();
        } catch (SQLException e) {
            // This should not happen. A J2EE compatiable JDBC driver is
            // required fully support metadata.
            throw MESSAGES.errorCheckingIfTableExists(tableName, e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(con);
        }
    }

    public static OldColumns getOldColumns(String tableName, DataSource dataSource) {
        Connection con = null;
        ResultSet rs = null;
        ArrayList columnNames = new ArrayList();
        ArrayList typeNames = new ArrayList();
        ArrayList columnSizes = new ArrayList();
        try {
            con = dataSource.getConnection();

            // (a j2ee spec compatible jdbc driver has to fully
            // implement the DatabaseMetaData)
            DatabaseMetaData dmd = con.getMetaData();
            String catalog = con.getCatalog();
            String schema = null;
            String quote = dmd.getIdentifierQuoteString();
            if (tableName.startsWith(quote)) {
                if (tableName.endsWith(quote) == false) {
                    throw MESSAGES.mismatchedQuoteTableName(tableName);
                }
                int quoteLength = quote.length();
                tableName = tableName.substring(quoteLength, tableName.length() - quoteLength);
                if (dmd.storesLowerCaseQuotedIdentifiers())
                    tableName = tableName.toLowerCase();
                else if (dmd.storesUpperCaseQuotedIdentifiers())
                    tableName = tableName.toUpperCase();
            } else {
                if (dmd.storesLowerCaseIdentifiers())
                    tableName = tableName.toLowerCase();
                else if (dmd.storesUpperCaseIdentifiers())
                    tableName = tableName.toUpperCase();
            }

            // Patch #927759: Split tablename into "schema" and "table" separated by '.'
            int dotIndex;
            if ((dotIndex = tableName.indexOf('.')) != -1) {
                // Yank out schema name ...
                schema = tableName.substring(0, dotIndex);
                tableName = tableName.substring(dotIndex + 1);
            }

            rs = dmd.getColumns(catalog, schema, tableName, null);
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                columnNames.add(columnName == null ? null : columnName.toUpperCase());
                typeNames.add(rs.getString("TYPE_NAME"));
                columnSizes.add(new Integer(rs.getInt("COLUMN_SIZE")));
            }
            return new OldColumns(columnNames, typeNames, columnSizes);

        } catch (SQLException e) {
            // This should not happen. A J2EE compatiable JDBC driver is
            // required fully support metadata.
            throw MESSAGES.errorGettingColumnNames(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(con);
        }
    }

    public static OldIndexes getOldIndexes(String tableName, DataSource dataSource) {
        tableName = unquote(tableName, dataSource);

        Connection con = null;
        ResultSet rs = null;
        ArrayList indexNames = new ArrayList();
        ArrayList columnNames = new ArrayList();
        ArrayList ascDesc = new ArrayList();
        try {
            con = dataSource.getConnection();

            // (a j2ee spec compatible jdbc driver has to fully
            // implement the DatabaseMetaData)
            DatabaseMetaData dmd = con.getMetaData();
            String catalog = con.getCatalog();
            String schema = null;
            if (dmd.storesLowerCaseIdentifiers())
                tableName = tableName.toLowerCase();
            else if (dmd.storesUpperCaseIdentifiers())
                tableName = tableName.toUpperCase();

            // Patch #927759: Split tablename into "schema" and "table" separated by '.'
            int dotIndex;
            if ((dotIndex = tableName.indexOf('.')) != -1) {
                // Yank out schema name ...
                schema = tableName.substring(0, dotIndex);
                tableName = tableName.substring(dotIndex + 1);
            }

            rs = dmd.getIndexInfo(catalog, schema, tableName, false, false);
            while (rs.next()) {
                indexNames.add(rs.getString("INDEX_NAME"));
                columnNames.add(rs.getString("COLUMN_NAME"));
                ascDesc.add(rs.getString("ASC_OR_DESC"));
            }
            return new OldIndexes(indexNames, columnNames, ascDesc);

        } catch (SQLException e) {
            // This should not happen. A J2EE compatiable JDBC driver is
            // required fully support metadata.
            throw MESSAGES.errorGettingColumnNames(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(con);
        }
    }

    public static String unquote(String tableName, DataSource ds) {
        Connection con = null;
        try {
            con = ds.getConnection();
            String quote = con.getMetaData().getIdentifierQuoteString();
            if (tableName.startsWith(quote)) {
                if (tableName.endsWith(quote) == false) {
                    throw MESSAGES.mismatchedQuoteTableName(tableName);
                }
                int quoteLength = quote.length();
                tableName = tableName.substring(quoteLength, tableName.length() - quoteLength);
            }
        } catch (SQLException e) {
            throw MESSAGES.failedToGetDataSourceConnection(e);
        } finally {
            JDBCUtil.safeClose(con);
        }
        return tableName;
    }

    private static JDBCType getJDBCType(JDBCFieldBridge field) {
        JDBCType type = field.getJDBCType();
        if (type != null && type.getColumnNames().length > 0) {
            return type;
        }
        return null;
    }

    public static void dropTable(DataSource dataSource,
                                 String tableName) {
        Logger log = Logger.getLogger("CLEANER");
        String sql = "DROP TABLE " + tableName;
        try {
            Connection con = null;
            Statement statement = null;
            try {
                // execute sql
                con = dataSource.getConnection();
                statement = con.createStatement();
                statement.executeUpdate(sql);
            } finally {
                // make sure to close the connection and statement before
                // comitting the transaction or XA will break
                JDBCUtil.safeClose(statement);
                JDBCUtil.safeClose(con);
            }
        } catch (Exception e) {
            throw MESSAGES.errorDroppingTable(tableName, e);
        }
        CmpLogger.ROOT_LOGGER.droppedTable(tableName);
    }

    /**
     * utility class to store the information returned by getOldColumns()
     */
    public static class OldColumns {
        private ArrayList columnNames;
        private ArrayList typeNames;
        private ArrayList columnSizes;

        private OldColumns(ArrayList cn, ArrayList tn, ArrayList cs) {
            columnNames = cn;
            typeNames = tn;
            columnSizes = cs;
        }

        public ArrayList getColumnNames() {
            return columnNames;
        }

        public ArrayList getTypeNames() {
            return typeNames;
        }

        public ArrayList getColumnSizes() {
            return columnSizes;
        }
    }

    /**
     * utility class to store the information returned by getOldColumns()
     */
    public static class OldIndexes {
        private ArrayList indexNames;
        private ArrayList columnNames;
        private ArrayList columnAscDesc;

        private OldIndexes(ArrayList in, ArrayList cn, ArrayList ad) {
            indexNames = in;
            columnNames = cn;
            columnAscDesc = ad;
        }

        public ArrayList getColumnNames() {
            return columnNames;
        }

        public ArrayList getIndexNames() {
            return indexNames;
        }

        public ArrayList getColumnAscDesc() {
            return columnAscDesc;
        }
    }

}
