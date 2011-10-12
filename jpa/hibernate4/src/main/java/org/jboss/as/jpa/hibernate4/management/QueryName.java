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

package org.jboss.as.jpa.hibernate4.management;

/**
 * Represents the Hibernate query name which is passed in as a parameter.  The displayQuery can be obtained which
 * has spaces and other symbols replaced with a textual description (which shouldn't be changed or localized.
 * The localization rule is so that one set of admin scripts will work against any back end system.  If it becomes
 * more important to localize the textual descriptions, care should be taken to avoid duplicate values when doing so.
 *
 * @author Scott Marlow
 */
public class QueryName {

    // query name as returned from hibernate Statistics.getQueries()
    private final String hibernateQuery;

    // query name transformed for display use (allowed to be ugly but should be unique)
    private final String displayQuery;

    // HQL symbol or operators
    private static final String SQL_NE = "<>";
    private static final String NE_BANG = "!=";
    private static final String NE_HAT = "^=";
    private static final String LE = "<=";
    private static final String GE = ">=";
    private static final String CONCAT= "||";
    private static final String LT ="<";
    private static final String EQ = "=";
    private static final String GT =">";
    private static final String OPEN= "(";
    private static final String CLOSE= ")";
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String PLUS = "+";
    private static final String MINUS = "-";
    private static final String STAR = "*";
    private static final String DIV = "/";
    private static final String MOD = "%";
    private static final String COLON = ":";
    private static final String PARAM= "?";
    private static final String COMMA =",";
    private static final String SPACE=" ";
    private static final String TAB="\t";
    private static final String NEWLINE = "\n";
    private static final String LINEFEED = "\r";
    private static final String QUOTE = "'";
    private static final String DQUOTE = "\"";
    private static final String TICK = "`";
    private static final String OPEN_BRACE = "{";
    private static final String CLOSE_BRACE = "}";
    private static final String HAT = "^";
    private static final String AMPERSAND = "&";

    // textual representation (not to be localized as we don't won't duplication between any of the values)
    private static final String NOT_EQUAL__ = "__not_equal__";
    private static final String BANG_NOT_EQUAL__ = "__bang_not_equal__";
    private static final String HAT_NOT_EQUAL__ = "__hat_not_equal__";
    private static final String LESS_THAN_EQUAL__ = "__less_than_equal__";
    private static final String GREATER_THAN_EQUAL__ = "__greater_than_equal__";
    private static final String CONCAT__ = "__concat__";
    private static final String LESS_THAN__ = "__less_than__";
    private static final String EQUAL__ = "__equal__";
    private static final String GREATER__ = "__greater__";
    private static final String LEFT_PAREN__ = "__left_paren__";
    private static final String RIGHT_PAREN__ = "__right_paren__";
    private static final String LEFT_BRACKET__ = "__left_bracket__";
    private static final String RIGHT_BRACKET__ = "__right_bracket__";
    private static final String PLUS__ = "__plus__";
    private static final String MINUS__ = "__minus__";
    private static final String STAR__ = "__star__";
    private static final String DIVIDE__ = "__divide__";
    private static final String MODULUS__ = "__modulus__";
    private static final String COLON__ = "__colon__";
    private static final String PARAM__ = "__param__";
    private static final String COMMA__ = "__comma__";
    private static final String SPACE__ = "__space__";
    private static final String TAB__ = "__tab__";
    private static final String NEWLINE__ = "__newline__";
    private static final String LINEFEED__ = "__linefeed__";
    private static final String QUOTE__ = "__quote__";
    private static final String DQUOTE__ = "__double_quote__";
    private static final String TICK__ = "__tick__";
    private static final String OPEN_BRACE__ = "__left_brace__";
    private static final String CLOSE_BRACE__ = "__right_brace__";
    private static final String HAT__ = "__hat__";
    private static final String AMPERSAND__ = "__ampersand__";

    public static QueryName queryName(String query) {
        return new QueryName(query);
    }

    /**
     * Construct
     * @param query
     */
    public QueryName(String query) {
        hibernateQuery = query;
        displayQuery = displayable(query);

    }

    public String getOriginalName() {
        return hibernateQuery;
    }

    public String getDisplayName() {
        return displayQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryName queryName = (QueryName) o;

        if (displayQuery != null ? !displayQuery.equals(queryName.displayQuery) : queryName.displayQuery != null)
            return false;
        if (hibernateQuery != null ? !hibernateQuery.equals(queryName.hibernateQuery) : queryName.hibernateQuery != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hibernateQuery != null ? hibernateQuery.hashCode() : 0;
        result = 31 * result + (displayQuery != null ? displayQuery.hashCode() : 0);
        return result;
    }

    /**
     * transform a Hibernate HQL query into something that can be displayed/used for management operations
     * @param query
     * @return
     */
    private String displayable(String query) {
        if (query == null ||
            query.length() == 0) {
            return query;
        }
        // handle two character transforms first
        query = query.replace(SQL_NE, NOT_EQUAL__);
        query = query.replace(NE_BANG, BANG_NOT_EQUAL__);
        query = query.replace(NE_HAT, HAT_NOT_EQUAL__);
        query = query.replace(LE, LESS_THAN_EQUAL__);
        query = query.replace(GE, GREATER_THAN_EQUAL__);
        query = query.replace(CONCAT, CONCAT__);
        query = query.replace(LT, LESS_THAN__);
        query = query.replace(EQ, EQUAL__);
        query = query.replace(GT, GREATER__);
        query = query.replace(OPEN, LEFT_PAREN__);
        query = query.replace(CLOSE, RIGHT_PAREN__);
        query = query.replace(OPEN_BRACKET, LEFT_BRACKET__);
        query = query.replace(CLOSE_BRACKET, RIGHT_BRACKET__);
        query = query.replace(PLUS, PLUS__);
        query = query.replace(MINUS, MINUS__);
        query = query.replace(STAR, STAR__);
        query = query.replace(DIV, DIVIDE__);
        query = query.replace(MOD, MODULUS__);
        query = query.replace(COLON, COLON__);
        query = query.replace(PARAM, PARAM__);
        query = query.replace(COMMA, COMMA__);
        query = query.replace(SPACE, SPACE__);
        query = query.replace(TAB, TAB__);
        query = query.replace(NEWLINE, NEWLINE__);
        query = query.replace(LINEFEED, LINEFEED__);
        query = query.replace(QUOTE, QUOTE__);
        query = query.replace(DQUOTE, DQUOTE__);
        query = query.replace(TICK, TICK__);
        query = query.replace(OPEN_BRACE, OPEN_BRACE__);
        query = query.replace(CLOSE_BRACE, CLOSE_BRACE__);
        query = query.replace(HAT, HAT__);
        query = query.replace(AMPERSAND, AMPERSAND__);
        return query;
    }

}
