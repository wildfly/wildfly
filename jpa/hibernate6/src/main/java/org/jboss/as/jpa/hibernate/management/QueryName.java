/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.management;

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

    // textual representation (not to be localized as we don't won't duplication between any of the values)
    private static final String NOT_EQUAL__ = "_not_equal_";
    private static final String BANG_NOT_EQUAL__ = "_bang_not_equal_";
    private static final String HAT_NOT_EQUAL__ = "_hat_not_equal_";
    private static final String LESS_THAN_EQUAL__ = "_less_than_equal_";
    private static final String GREATER_THAN_EQUAL__ = "_greater_than_equal_";
    private static final String CONCAT__ = "_concat_";
    private static final String LESS_THAN__ = "_less_than_";
    private static final String EQUAL__ = "_equal_";
    private static final String GREATER__ = "_greater_";
    private static final String LEFT_PAREN__ = "_left_paren_";
    private static final String RIGHT_PAREN__ = "_right_paren_";
    private static final String LEFT_BRACKET__ = "_left_bracket_";
    private static final String RIGHT_BRACKET__ = "_right_bracket_";
    private static final String PLUS__ = "_plus_";
    private static final String MINUS__ = "_minus_";
    private static final String STAR__ = "_star_";
    private static final String DIVIDE__ = "_divide_";
    private static final String MODULUS__ = "_modulus_";
    private static final String COLON__ = "_colon_";
    private static final String PARAM__ = "_param_";
    private static final String COMMA__ = "_comma_";
    private static final String SPACE__ = "_space_";
    private static final String TAB__ = "_tab_";
    private static final String NEWLINE__ = "_newline_";
    private static final String LINEFEED__ = "_linefeed_";
    private static final String QUOTE__ = "_quote_";
    private static final String DQUOTE__ = "_double_quote_";
    private static final String TICK__ = "_tick_";
    private static final String OPEN_BRACE__ = "_left_brace_";
    private static final String CLOSE_BRACE__ = "_right_brace_";
    private static final String HAT__ = "_hat_";
    private static final String AMPERSAND__ = "_ampersand_";

    public static QueryName queryName(String query) {
        return new QueryName(query);
    }

    /**
     * Construct
     *
     * @param query
     */
    private QueryName(String query) {
        hibernateQuery = query;
        displayQuery = displayable(query);
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
     *
     * @param query
     * @return
     */
    private String displayable(String query) {
        if (query == null ||
                query.isEmpty()) {
            return query;
        }
        final int queryLength = query.length();
        final StringBuilder out = new StringBuilder(queryLength);
        for (int index = 0; index < queryLength; index++) {
            final char current = query.charAt(index);
            final char next = index + 1 < queryLength ? query.charAt(index + 1) : 0;
            switch (current) {
                case '<':
                    if (next == '>') {
                        index++;
                        out.append(NOT_EQUAL__);
                    } else if (next == '=') {
                        index++;
                        out.append(LESS_THAN_EQUAL__);
                    } else {
                        out.append(LESS_THAN__);
                    }
                    break;
                case '!':
                    if (next == '=') {
                        index++;
                        out.append(BANG_NOT_EQUAL__);
                    } else {
                        out.append(current);
                    }
                    break;
                case '^':
                    if (next == '=') {
                        index++;
                        out.append(HAT_NOT_EQUAL__);
                    } else {
                        out.append(HAT__);
                    }
                    break;
                case '>':
                    if (next == '=') {
                        index++;
                        out.append(GREATER_THAN_EQUAL__);
                    } else {
                        out.append(GREATER__);
                    }
                    break;
                case '|':
                    if (next == '!') {
                        index++;
                        out.append(CONCAT__);
                    } else {
                        out.append(current);
                    }
                    break;
                case '=':
                    out.append(EQUAL__);
                    break;
                case '(':
                    out.append(LEFT_PAREN__);
                    break;
                case ')':
                    out.append(RIGHT_PAREN__);
                    break;
                case '[':
                    out.append(LEFT_BRACKET__);
                    break;
                case ']':
                    out.append(RIGHT_BRACKET__);
                    break;
                case '{':
                    out.append(OPEN_BRACE__);
                    break;
                case '}':
                    out.append(CLOSE_BRACE__);
                    break;
                case '+':
                    out.append(PLUS__);
                    break;
                case '-':
                    out.append(MINUS__);
                    break;
                case '*':
                    out.append(STAR__);
                    break;
                case '/':
                    out.append(DIVIDE__);
                    break;
                case '%':
                    out.append(MODULUS__);
                    break;
                case ':':
                    out.append(COLON__);
                    break;
                case '?':
                    out.append(PARAM__);
                    break;
                case ',':
                    out.append(COMMA__);
                    break;
                case ' ':
                    out.append(SPACE__);
                    break;
                case '\t':
                    out.append(TAB__);
                    break;
                case '\n':
                    out.append(NEWLINE__);
                    break;
                case '\r':
                    out.append(LINEFEED__);
                    break;
                case '\'':
                    out.append(QUOTE__);
                    break;
                case '\"':
                    out.append(DQUOTE__);
                    break;
                case '`':
                    out.append(TICK__);
                    break;
                case '&':
                    out.append(AMPERSAND__);
                    break;
                default:
                    out.append(current);
                    break;
            }
        }
        return out.toString();
    }

    /*
        public static void main(String[] args) {
            String testvalue =
                    "query name select * from mytable where mytable.id <> != ^= = >= , , , , , , ,\"\" {}";
            for (int loop = 0; loop < 10; loop++) {
                testvalue = testvalue + testvalue;
            }
            for (int loop = 0; loop < 100; loop++) {
                QueryName queryName = new QueryName(testvalue);
            }
        }
    */
}
