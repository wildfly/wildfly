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

import java.util.Collection;
import java.util.HashMap;


/**
 * Immutable class which holds a map between Java Classes and JDBCMappingMetaData.
 *
 * @author John Bailey
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:loubyansky@ua.fm">Alex Loubyansky</a>
 */
public final class JDBCTypeMappingMetaData {
    private static final String[] PRIMITIVES = {
            "boolean", "byte", "char", "short", "int", "long", "float", "double"
    };

    private static final String[] PRIMITIVE_CLASSES = {
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
            "java.lang.Short", "java.lang.Integer", "java.lang.Long",
            "java.lang.Float", "java.lang.Double"
    };

    public static final String CONCAT = "concat";
    public static final String SUBSTRING = "substring";
    public static final String LCASE = "lcase";
    public static final String UCASE = "ucase";
    public static final String LENGTH = "length";
    public static final String LOCATE = "locate";
    public static final String ABS = "abs";
    public static final String SQRT = "sqrt";
    public static final String COUNT = "count";
    public static final String MOD = "mod";

    public static JDBCFunctionMappingMetaData COUNT_FUNC;
    public static JDBCFunctionMappingMetaData MAX_FUNC;
    public static JDBCFunctionMappingMetaData MIN_FUNC;
    public static JDBCFunctionMappingMetaData AVG_FUNC;
    public static JDBCFunctionMappingMetaData SUM_FUNC;

    static {
        try {
            COUNT_FUNC = new JDBCFunctionMappingMetaData("count", "count(?1 ?2)");
            MAX_FUNC = new JDBCFunctionMappingMetaData("max", "max(?1 ?2)");
            MIN_FUNC = new JDBCFunctionMappingMetaData("min", "min(?1 ?2)");
            AVG_FUNC = new JDBCFunctionMappingMetaData("avg", "avg(?1 ?2)");
            SUM_FUNC = new JDBCFunctionMappingMetaData("sum", "sum(?1 ?2)");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private String name;

    private final HashMap<String, JDBCMappingMetaData> mappings = new HashMap<String, JDBCMappingMetaData>();

    private final HashMap<String, JDBCFunctionMappingMetaData> functionMappings = new HashMap<String, JDBCFunctionMappingMetaData>();

    private String aliasHeaderPrefix;
    private String aliasHeaderSuffix;
    private int aliasMaxLength;

    private boolean subquerySupported;

    private String trueMapping;
    private String falseMapping;
    private int maxKeysInDelete;

    private JDBCFunctionMappingMetaData rowLocking = null;
    private JDBCFunctionMappingMetaData fkConstraint = null;
    private JDBCFunctionMappingMetaData pkConstraint = null;
    private JDBCFunctionMappingMetaData autoIncrement = null;
    private JDBCFunctionMappingMetaData addColumn = null;
    private JDBCFunctionMappingMetaData dropColumn = null;
    private JDBCFunctionMappingMetaData alterColumn = null;

    public JDBCTypeMappingMetaData() {
        addDefaultFunctionMapping();
    }

    /**
     * Gets the name of this mapping. The mapping name used to differentiate this
     * mapping from other mappings and the mapping the application used is
     * retrieved by name.
     *
     * @return the name of this mapping.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the prefix for that is used when generating an alias header.  An
     * alias header is prepended to a generated table alias to prevent name
     * collisions. An alias header is constructed as follows:
     * aliasHeaderPrefix + int_counter + aliasHeaderSuffix
     *
     * @return the prefix for alias headers
     */
    public String getAliasHeaderPrefix() {
        return aliasHeaderPrefix;
    }

    /**
     * Gets the suffix for that is used when generating an alias header.  An
     * alias header is prepended to a generated table alias to prevent name
     * collisions. An alias header is constructed as follows:
     * aliasHeaderPrefix + int_counter + aliasHeaderSuffix
     *
     * @return the suffix for alias headers
     */
    public String getAliasHeaderSuffix() {
        return aliasHeaderSuffix;
    }

    /**
     * Gets maximum length of a table alias.
     * An alias is constructed as follows: aliasHeader + ejb_ql_identifier_path
     *
     * @return the maximum length that a table alias can be
     */
    public int getAliasMaxLength() {
        return aliasMaxLength;
    }

    /**
     * Does this type mapping support subqueries?
     */
    public boolean isSubquerySupported() {
        return subquerySupported;
    }

    /**
     * Gets the value to which the boolean true value in EJB-QL will be mapped.
     */
    public String getTrueMapping() {
        return trueMapping;
    }

    /**
     * Gets the value to which the boolean false value in EJB-QL will be mapped.
     */
    public String getFalseMapping() {
        return falseMapping;
    }

    public int getMaxKeysInDelete() {
        return maxKeysInDelete;
    }

    public JDBCMappingMetaData getTypeMappingMetaData(Class type) {
        String javaType = type.getName();

        // Check primitive first
        for (int i = 0; i < PRIMITIVES.length; i++) {
            if (javaType.equals(PRIMITIVES[i])) {
                // Translate into class
                javaType = PRIMITIVE_CLASSES[i];
                break;
            }
        }

        // Check other types
        JDBCMappingMetaData mapping = (JDBCMappingMetaData) mappings.get(javaType);

        // if not found, return mapping for java.lang.object
        if (mapping == null) {
            mapping = (JDBCMappingMetaData) mappings.get("java.lang.Object");
        }

        return mapping;
    }

    public JDBCFunctionMappingMetaData getFunctionMapping(String name) {
        JDBCFunctionMappingMetaData funcMapping = (JDBCFunctionMappingMetaData) functionMappings.get(name.toLowerCase());
        if (funcMapping == null)
            throw new IllegalStateException("Function " + name + " is not defined for " + this.name);
        return funcMapping;
    }


    /**
     * Returns rowLocking SQL template.
     */
    public JDBCFunctionMappingMetaData getRowLockingTemplate() {
        return rowLocking;
    }

    /**
     * Returns pk constraint SQL template.
     */
    public JDBCFunctionMappingMetaData getPkConstraintTemplate() {
        return pkConstraint;
    }

    /**
     * Returns fk constraint SQL template.
     */
    public JDBCFunctionMappingMetaData getFkConstraintTemplate() {
        return fkConstraint;
    }

    /**
     * Returns auto increment SQL template.
     */
    public JDBCFunctionMappingMetaData getAutoIncrementTemplate() {
        return autoIncrement;
    }

    /**
     * Returns add column SQL template.
     */
    public JDBCFunctionMappingMetaData getAddColumnTemplate() {
        return addColumn;
    }

    /**
     * Returns auto increment SQL template.
     */
    public JDBCFunctionMappingMetaData getDropColumnTemplate() {
        return dropColumn;
    }

    /**
     * Returns auto increment SQL template.
     */
    public JDBCFunctionMappingMetaData getAlterColumnTemplate() {
        return alterColumn;
    }

    public Collection<JDBCMappingMetaData> getMappings() {
        return mappings.values();
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setAddColumnTemplate(final JDBCFunctionMappingMetaData addColumn) {
        this.addColumn = addColumn;
    }

    public void setRowLockingTemplate(final JDBCFunctionMappingMetaData rowLocking) {
        this.rowLocking = rowLocking;
    }

    public void setPKConstraintTemplate(final JDBCFunctionMappingMetaData pkConstraint) {
        this.pkConstraint = pkConstraint;
    }

    public void setFKConstraintTemplate(final JDBCFunctionMappingMetaData fkConstraint) {
        this.fkConstraint = fkConstraint;
    }

    public void setAlterColumnTemplate(final JDBCFunctionMappingMetaData alterColumn) {
        this.alterColumn = alterColumn;
    }

    public void setAliasHeaderPrefix(final String aliasHeaderPrefix) {
        this.aliasHeaderPrefix = aliasHeaderPrefix;
    }

    public void setAliasHeaderSuffix(final String aliasHeaderSuffix) {
        this.aliasHeaderSuffix = aliasHeaderSuffix;
    }

    public void setAliasMaxLength(final int aliasMaxLength) {
        this.aliasMaxLength = aliasMaxLength;
    }

    public void setAutoIncrementTemplate(final JDBCFunctionMappingMetaData autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public void setDropColumnTemplate(final JDBCFunctionMappingMetaData dropColumn) {
        this.dropColumn = dropColumn;
    }

    public void setFalseMapping(final String falseMapping) {
        this.falseMapping = falseMapping;
    }

    public void setMaxKeysInDelete(final int maxKeys) {
        this.maxKeysInDelete = maxKeys;
    }

    public void setSubQuerySupported(final Boolean subquerySupported) {
        this.subquerySupported = subquerySupported;
    }

    public void setTrueMapping(final String trueMapping) {
        this.trueMapping = trueMapping;
    }

    private void addDefaultFunctionMapping() {
        JDBCFunctionMappingMetaData function;

        // concat
        function = new JDBCFunctionMappingMetaData("concat",
                new String[]{
                        "{fn concat(",
                        ", ",
                        ")}"
                },
                new int[]{0, 1});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // substring
        function = new JDBCFunctionMappingMetaData("substring",
                new String[]{
                        "{fn substring(",
                        ", ",
                        ", ",
                        ")}"
                },
                new int[]{0, 1, 2});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // lcase
        function = new JDBCFunctionMappingMetaData("lcase",
                new String[]{
                        "{fn lcase(",
                        ")}"
                },
                new int[]{0});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // ucase
        function = new JDBCFunctionMappingMetaData("ucase",
                new String[]{
                        "{fn ucase(",
                        ")}"
                },
                new int[]{0});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // length
        function = new JDBCFunctionMappingMetaData("length",
                new String[]{
                        "{fn length(",
                        ")}"
                },
                new int[]{0});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // locate
        function = new JDBCFunctionMappingMetaData("locate",
                new String[]{
                        "{fn locate(",
                        ", ",
                        ", ",
                        ")}"
                },
                new int[]{0, 1, 2});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // abs
        function = new JDBCFunctionMappingMetaData("abs",
                new String[]{
                        "{fn abs(",
                        ")}"
                },
                new int[]{0});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // sqrt
        function = new JDBCFunctionMappingMetaData("sqrt",
                new String[]{
                        "{fn sqrt(",
                        ")}"
                },
                new int[]{0});
        functionMappings.put(function.getFunctionName().toLowerCase(), function);

        // mod
        function = new JDBCFunctionMappingMetaData("mod", "mod(?1, ?2)");
        functionMappings.put(function.getFunctionName().toLowerCase(), function);
    }

    public void addMapping(final JDBCMappingMetaData jdbcMappingMetaData) {
        this.mappings.put(jdbcMappingMetaData.getJavaType(), jdbcMappingMetaData);
    }

    public void addFunctionMapping(final JDBCFunctionMappingMetaData functionMapping) {
        this.functionMappings.put(functionMapping.getFunctionName().toLowerCase(), functionMapping);
    }
}
