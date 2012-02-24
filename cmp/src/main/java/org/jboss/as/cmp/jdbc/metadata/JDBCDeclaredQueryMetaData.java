/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

import java.lang.reflect.Method;
import java.util.Map;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;


/**
 * Immutable class contains information about a declared query.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCDeclaredQueryMetaData implements JDBCQueryMetaData {
    /**
     * The method to which this query is bound.
     */
    private final Method method;

    /**
     * The user specified additional columns to be added to the select clause.
     */
    private final String additionalColumns;

    /**
     * The user specified from clause.
     */
    private final String from;

    /**
     * The user specified where clause.
     */
    private final String where;

    /**
     * The user specified order clause.
     */
    private final String order;

    /**
     * The other clause is appended to the end of the sql.  This is useful for
     * hints to the query engine.
     */
    private final String other;

    /**
     * Should the select be DISTINCT?
     */
    private final boolean distinct;

    /**
     * The name of the ejb from which the field will be selected.
     */
    private final String ejbName;

    /**
     * The name of the cmp-field to be selected.
     */
    private final String fieldName;

    /**
     * The alias that is used for the main select table.
     */
    private final String alias;
    /**
     * Read ahead meta data.
     */
    private final JDBCReadAheadMetaData readAhead;

    /**
     * Should the query return Local or Remote beans.
     */
    private final boolean resultTypeMappingLocal;

    private final Class compiler;

    private final boolean lazyResultSetLoading;

    /**
     * Constructs a JDBCDeclaredQueryMetaData which is defined by the
     * declared-sql xml element and is invoked by the specified method.
     * Inherits unspecified values from the defaults.
     *
     * @param defaults  the default values to use
     * @param readAhead the read-ahead properties for this query
     */
    public JDBCDeclaredQueryMetaData(JDBCDeclaredQueryMetaData defaults,
                                     JDBCReadAheadMetaData readAhead,
                                     Class compiler,
                                     boolean lazyResultSetLoading) {
        this.method = defaults.getMethod();
        this.readAhead = readAhead;

        this.from = defaults.getFrom();
        this.where = defaults.getWhere();
        this.order = defaults.getOrder();
        this.other = defaults.getOther();

        this.resultTypeMappingLocal = defaults.isResultTypeMappingLocal();

        this.distinct = defaults.isSelectDistinct();
        this.ejbName = defaults.getEJBName();
        this.fieldName = defaults.getFieldName();
        this.alias = defaults.getAlias();
        this.additionalColumns = defaults.getAdditionalColumns();

        this.compiler = compiler;
        this.lazyResultSetLoading = lazyResultSetLoading;
    }


    public JDBCDeclaredQueryMetaData(boolean isResultTypeMappingLocal,
                                     Method method,
                                     JDBCReadAheadMetaData readAhead,
                                     Class compiler,
                                     boolean lazyResultSetLoading,
                                     Map<String, String> props) {
        this.compiler = compiler;
        this.lazyResultSetLoading = lazyResultSetLoading;

        this.method = method;
        this.readAhead = readAhead;

        from = props.get("from");
        where = props.get("where");
        order = props.get("order");
        other = props.get("other");

        this.resultTypeMappingLocal = isResultTypeMappingLocal;

        if (!props.isEmpty()) {
            // should select use distinct?
            distinct = props.get("distinct") != null;

            if (method.getName().startsWith("ejbSelect")) {
                ejbName = props.get("ejb-name");
                fieldName = props.get("field-name");
            } else {
                // the ejb-name and field-name elements are not allowed for finders
                if (props.get("ejb-name") != null) {
                    throw MESSAGES.declaredSqlElementNotAllowed("ejb-name");
                }
                if (props.get("field-name") != null) {
                    throw MESSAGES.declaredSqlElementNotAllowed("field-name");
                }
                ejbName = null;
                fieldName = null;
            }
            alias = props.get("alias");
            additionalColumns = props.get("additional-columns");
        } else {
            if (method.getName().startsWith("ejbSelect")) {
                throw MESSAGES.declaredSqlElementNotAllowed("select");
            }
            distinct = false;
            ejbName = null;
            fieldName = null;
            alias = null;
            additionalColumns = null;
        }
    }

    // javadoc in parent class
    public Method getMethod() {
        return method;
    }

    // javadoc in parent class
    public boolean isResultTypeMappingLocal() {
        return resultTypeMappingLocal;
    }

    /**
     * Gets the read ahead metadata for the query.
     *
     * @return the read ahead metadata for the query.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return readAhead;
    }

    public Class getQLCompilerClass() {
        return compiler;
    }

    /**
     * Gets the sql FROM clause of this query.
     *
     * @return a String which contains the sql FROM clause
     */
    public String getFrom() {
        return from;
    }

    /**
     * Gets the sql WHERE clause of this query.
     *
     * @return a String which contains the sql WHERE clause
     */
    public String getWhere() {
        return where;
    }

    /**
     * Gets the sql ORDER BY clause of this query.
     *
     * @return a String which contains the sql ORDER BY clause
     */
    public String getOrder() {
        return order;
    }

    /**
     * Gets other sql code which is appended to the end of the query.
     * This is useful for supplying hints to the query engine.
     *
     * @return a String which contains additional sql code which is
     *         appended to the end of the query
     */
    public String getOther() {
        return other;
    }

    /**
     * Should the select be DISTINCT?
     *
     * @return true if the select clause should contain distinct
     */
    public boolean isSelectDistinct() {
        return distinct;
    }

    /**
     * The name of the ejb from which the field will be selected.
     *
     * @return the name of the ejb from which a field will be selected, or null
     *         if returning a whole ejb
     */
    public String getEJBName() {
        return ejbName;
    }

    /**
     * The name of the cmp-field to be selected.
     *
     * @return the name of the cmp-field to be selected or null if returning a
     *         whole ejb
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * The alias that is used for the select table.
     *
     * @return the alias that is used for the table from which the entity or
     *         field is selected.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Additional columns that should be added to the select clause. For example,
     * columns that are used in an order by clause.
     *
     * @return additional columns that should be added to the select clause
     */
    public String getAdditionalColumns() {
        return additionalColumns;
    }

    public boolean isLazyResultSetLoading() {
        return lazyResultSetLoading;
    }

    /**
     * Compares this JDBCDeclaredQueryMetaData against the specified object.
     * Returns true if the objects are the same. Two JDBCDeclaredQueryMetaData
     * are the same if they are both invoked by the same method.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument; false
     *         otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCDeclaredQueryMetaData) {
            return ((JDBCDeclaredQueryMetaData) o).method.equals(method);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCDeclaredQueryMetaData. The hashcode is
     * computed by the method which invokes this query.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns a string describing this JDBCDeclaredQueryMetaData. The exact
     * details of the representation are unspecified and subject to change,
     * but the following may be regarded as typical:
     * <p/>
     * "[JDBCDeclaredQueryMetaData: method=public org.foo.User findByName(
     * java.lang.String)]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCDeclaredQueryMetaData : method=" + method + "]";
    }

    private static String nullIfEmpty(String s) {
        if (s != null && s.trim().length() == 0) {
            s = null;
        }
        return s;
    }
}
