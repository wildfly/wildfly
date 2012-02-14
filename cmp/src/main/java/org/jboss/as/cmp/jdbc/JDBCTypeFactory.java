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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldPropertyMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCUserTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValueClassMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValuePropertyMetaData;

//import org.jboss.logging.Logger;

/**
 * JDBCTypeFactory maps Java Classes to JDBCType objects.  The main job of
 * this class is to flatten the JDBCValueClassMetaData into columns.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCTypeFactory {
    //private static final Logger log = Logger.getLogger(JDBCTypeFactory.class);

    //
    // Default CMPFieldStateFactory implementations
    //

    /**
     * This implementation uses field's value as its state.
     */
    public static CMPFieldStateFactory EQUALS = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            return fieldValue;
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            return state == null ? fieldValue == null : state.equals(fieldValue);
        }
    };

    /**
     * This implementation will always suppose that the state is invalid unless
     * both states are null.
     */
    private static CMPFieldStateFactory INVALID_UNLESS_NULL = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            return fieldValue;
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            return state == null ? fieldValue == null : false;
        }
    };

    /**
     * Field state factory for java.util.Map implementations. The state is
     * a deep copy of the value.
     */
    private static CMPFieldStateFactory MAP = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            return cloneValue(fieldValue, Map.class);
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            return (state == null ? fieldValue == null : state.equals(fieldValue));
        }
    };

    /**
     * Field state factory for java.util.List implementations. The state is
     * a deep copy of the value.
     */
    private static CMPFieldStateFactory LIST = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            return cloneValue(fieldValue, Collection.class);
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            return (state == null ? fieldValue == null : state.equals(fieldValue));
        }
    };

    /**
     * Field state factory for java.util.Set implementations. The state is
     * a deep copy of the value.
     */
    private static CMPFieldStateFactory SET = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            return cloneValue(fieldValue, Collection.class);
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            return (state == null ? fieldValue == null : state.equals(fieldValue));
        }
    };

    /**
     * Field state factory for arrays. The state is a deep copy of the value.
     */
    private static CMPFieldStateFactory ARRAY = new CMPFieldStateFactory() {
        public Object getFieldState(Object fieldValue) {
            Object state = null;
            if (fieldValue != null) {
                int length = Array.getLength(fieldValue);
                state = Array.newInstance(fieldValue.getClass().getComponentType(), length);
                System.arraycopy(fieldValue, 0, state, 0, length);
            }
            return state;
        }

        public boolean isStateValid(Object state, Object fieldValue) {
            boolean valid;
            if (state == null) {
                valid = fieldValue == null;
            } else {
                if (fieldValue == null) {
                    valid = false;
                } else {
                    int stateLength = Array.getLength(state);
                    if (stateLength != Array.getLength(fieldValue)) {
                        valid = false;
                    } else {
                        valid = true;
                        for (int i = 0; i < stateLength; ++i) {
                            Object stateEl = Array.get(state, i);
                            Object valueEl = Array.get(fieldValue, i);
                            valid = (stateEl == null ? valueEl == null : stateEl.equals(valueEl));
                            if (!valid) {
                                break;
                            }
                        }
                    }
                }
            }
            return valid;
        }
    };

    //
    // Static
    //

    public static CMPFieldStateFactory getCMPFieldStateFactory(JDBCTypeFactory factory,
                                                                     String implClassName,
                                                                     Class clazz) {
        CMPFieldStateFactory stateFactory;

        // if the state factory is not provided on the field level use the one from the user type mapping if any
        if (implClassName == null) {
            JDBCUserTypeMappingMetaData userMapping = (JDBCUserTypeMappingMetaData) factory.userTypeMappings.get(clazz.getName());
            if (userMapping != null) {
                implClassName = userMapping.getStateFactory();
            }
        }

        if (implClassName != null) {
            try {
                Class implClass = TCLAction.UTIL.getContextClassLoader().loadClass(implClassName);
                stateFactory = (CMPFieldStateFactory) implClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not load state factory class: " + implClassName);
            } catch (Exception e) {
                throw new RuntimeException("Failed instantiate state factory: " + implClassName);
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            stateFactory = MAP;
        } else if (List.class.isAssignableFrom(clazz)) {
            stateFactory = LIST;
        } else if (Set.class.isAssignableFrom(clazz)) {
            stateFactory = SET;
        } else if (clazz.isArray()) {
            stateFactory = ARRAY;
        } else if (usedWithEqualsStateFactory(clazz)) {
            stateFactory = EQUALS;
        } else {
            stateFactory = INVALID_UNLESS_NULL;
        }
        return stateFactory;
    }

    public static boolean checkDirtyAfterGet(JDBCTypeFactory factory, byte checkDirtyAfterGet, Class fieldType) {
        boolean result;
        if (checkDirtyAfterGet == JDBCCMPFieldMetaData.CHECK_DIRTY_AFTER_GET_NOT_PRESENT) {
            JDBCUserTypeMappingMetaData userMapping = (JDBCUserTypeMappingMetaData) factory.
                    userTypeMappings.get(fieldType.getName());
            if (userMapping != null &&
                    userMapping.checkDirtyAfterGet() != JDBCCMPFieldMetaData.CHECK_DIRTY_AFTER_GET_NOT_PRESENT) {
                result = userMapping.checkDirtyAfterGet() == JDBCCMPFieldMetaData.CHECK_DIRTY_AFTER_GET_TRUE;
            } else {
                result = !isDefaultImmutable(fieldType);
            }
        } else {
            result = checkDirtyAfterGet == JDBCCMPFieldMetaData.CHECK_DIRTY_AFTER_GET_TRUE;
        }
        return result;
    }

    private static Object cloneValue(Object fieldValue, Class argType) {
        if (fieldValue == null) {
            return null;
        }

        Class valueType = fieldValue.getClass();
        Constructor ctor;
        try {
            ctor = valueType.getConstructor(new Class[]{argType});
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Failed to find a ctor in " + valueType +
                            " that takes an instance of " + argType + " as an argument."
            );
        }

        try {
            return ctor.newInstance(new Object[]{fieldValue});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create an instance of " + valueType +
                            " with the " + fieldValue + " as a ctor argument"
            );
        }
    }

    private static boolean usedWithEqualsStateFactory(Class clazz) {
        return
                isDefaultImmutable(clazz) ||
                        clazz == java.util.Date.class ||
                        clazz == java.sql.Date.class ||
                        clazz == java.sql.Time.class ||
                        clazz == java.sql.Timestamp.class;
    }

    private static boolean isDefaultImmutable(Class clazz) {
        boolean result = false;
        if (clazz.isPrimitive()
                || clazz == Boolean.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == Character.class
                || clazz == String.class
                || clazz == java.math.BigInteger.class
                || clazz == java.math.BigDecimal.class
                ) {
            result = true;
        }
        return result;
    }

    //
    // Attributes
    //

    // the type mapping to use with the specified database
    private final JDBCTypeMappingMetaData typeMapping;

    // all known complex types by java class type
    private final Map complexTypes = new HashMap();
    private final Map mappedSimpleTypes = new HashMap();

    /**
     * user types mappings
     */
    private final Map userTypeMappings;

    public JDBCTypeFactory(JDBCTypeMappingMetaData typeMapping,
                           Collection valueClasses,
                           Map userTypeMappings) {
        this.typeMapping = typeMapping;
        this.userTypeMappings = userTypeMappings;

        HashMap valueClassesByType = new HashMap();
        for (Iterator i = valueClasses.iterator(); i.hasNext(); ) {
            JDBCValueClassMetaData valueClass = (JDBCValueClassMetaData) i.next();
            valueClassesByType.put(valueClass.getJavaType(), valueClass);
        }


        // convert the value class meta data to a jdbc complex type
        for (Iterator i = valueClasses.iterator(); i.hasNext(); ) {
            JDBCValueClassMetaData valueClass = (JDBCValueClassMetaData) i.next();
            JDBCTypeComplex type = createTypeComplex(valueClass, valueClassesByType);
            complexTypes.put(valueClass.getJavaType(), type);
        }

        Iterator i = typeMapping.getMappings().iterator();
        while (i.hasNext()) {
            JDBCMappingMetaData mapping = (JDBCMappingMetaData) i.next();

            String sqlType = mapping.getSqlType();
            int jdbcType = mapping.getJdbcType();
            Class javaType = loadClass(mapping.getJavaType());
            boolean notNull = javaType.isPrimitive();
            boolean autoIncrement = false;

            JDBCParameterSetter paramSetter;
            if (mapping.getParamSetter() != null) {
                paramSetter = (JDBCParameterSetter) newInstance(mapping.getParamSetter());
            } else {
                paramSetter = JDBCUtil.getParameterSetter(jdbcType, javaType);
            }

            JDBCResultSetReader resultReader;
            if (mapping.getResultReader() != null) {
                resultReader = (JDBCResultSetReader) newInstance(mapping.getResultReader());
            } else {
                resultReader = JDBCUtil.getResultSetReader(jdbcType, javaType);
            }

            JDBCTypeSimple type = new JDBCTypeSimple(
                    null, javaType, jdbcType, sqlType, notNull, autoIncrement, null, paramSetter, resultReader
            );
            mappedSimpleTypes.put(javaType, type);
        }
    }

    public JDBCType getJDBCType(Class javaType) {
        if (complexTypes.containsKey(javaType)) {
            return (JDBCTypeComplex) complexTypes.get(javaType);
        } else {
            JDBCTypeSimple type = (JDBCTypeSimple) mappedSimpleTypes.get(javaType);
            if (type == null) {
                JDBCUserTypeMappingMetaData userTypeMapping =
                        (JDBCUserTypeMappingMetaData) userTypeMappings.get(javaType.getName());
                Mapper mapper = null;
                if (userTypeMapping != null) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    try {
                        javaType = cl.loadClass(userTypeMapping.getMappedType());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Failed to load mapped type: " + userTypeMapping.getMappedType());
                    }

                    try {
                        mapper = (Mapper) newInstance(userTypeMapping.getMapper());
                    } catch (Throwable e) {
                        throw new IllegalStateException("Failed to create Mapper instance of " + userTypeMapping.getMapper());
                    }
                }

                JDBCMappingMetaData typeMappingMD = typeMapping.getTypeMappingMetaData(javaType);
                String sqlType = typeMappingMD.getSqlType();
                int jdbcType = typeMappingMD.getJdbcType();
                boolean notNull = javaType.isPrimitive();
                boolean autoIncrement = false;

                JDBCParameterSetter paramSetter;
                if (typeMappingMD.getParamSetter() != null) {
                    try {
                        paramSetter = (JDBCParameterSetter) newInstance(typeMappingMD.getParamSetter());
                    } catch (Throwable e) {
                        throw new IllegalStateException(e.getMessage());
                    }
                } else {
                    paramSetter = JDBCUtil.getParameterSetter(jdbcType, javaType);
                }

                JDBCResultSetReader resultReader;
                if (typeMappingMD.getResultReader() != null) {
                    try {
                        resultReader = (JDBCResultSetReader) newInstance(typeMappingMD.getResultReader());
                    } catch (Throwable e) {
                        throw new IllegalStateException(e.getMessage());
                    }
                } else {
                    resultReader = JDBCUtil.getResultSetReader(jdbcType, javaType);
                }

                type = new JDBCTypeSimple(
                        null, javaType, jdbcType, sqlType, notNull, autoIncrement, mapper, paramSetter, resultReader
                );
            }
            return type;
        }
    }

    public JDBCType getJDBCType(JDBCCMPFieldMetaData cmpField) {
        JDBCType fieldJDBCType;
        final Class fieldType = cmpField.getFieldType();
        if (complexTypes.containsKey(fieldType)) {
            fieldJDBCType = createTypeComplex(cmpField);
        } else {
            fieldJDBCType = createTypeSimple(cmpField);
        }
        return fieldJDBCType;
    }

    public int getJDBCTypeForJavaType(Class clazz) {
        return typeMapping.getTypeMappingMetaData(clazz).getJdbcType();
    }

    public JDBCTypeMappingMetaData getTypeMapping() {
        return typeMapping;
    }

    private JDBCTypeComplex createTypeComplex(
            JDBCValueClassMetaData valueClass,
            HashMap valueClassesByType) {
        // get the properties
        ArrayList propertyList = createComplexProperties(valueClass, valueClassesByType, new PropertyStack());

        // transform properties into an array
        JDBCTypeComplexProperty[] properties = new JDBCTypeComplexProperty[propertyList.size()];
        properties = (JDBCTypeComplexProperty[]) propertyList.toArray(properties);

        return new JDBCTypeComplex(properties, valueClass.getJavaType());
    }

    private JDBCTypeSimple createTypeSimple(JDBCCMPFieldMetaData cmpField) {
        String columnName = cmpField.getColumnName();
        Class javaType = cmpField.getFieldType();

        JDBCMappingMetaData typeMappingMD = typeMapping.getTypeMappingMetaData(javaType);
        String paramSetter = typeMappingMD.getParamSetter();
        String resultReader = typeMappingMD.getResultReader();

        int jdbcType;
        String sqlType = cmpField.getSQLType();
        if (sqlType != null) {
            jdbcType = cmpField.getJDBCType();
        } else {
            // get jdbcType and sqlType from typeMapping
            sqlType = typeMappingMD.getSqlType();
            jdbcType = typeMappingMD.getJdbcType();
        }

        boolean notNull = cmpField.isNotNull();
        boolean autoIncrement = cmpField.isAutoIncrement();

        Mapper mapper = null;
        JDBCUserTypeMappingMetaData userTypeMapping = (JDBCUserTypeMappingMetaData) userTypeMappings.get(javaType.getName());
        if (userTypeMapping != null) {
            String mappedTypeStr = userTypeMapping.getMappedType();
            try {
                final ClassLoader contextClassLoader = TCLAction.UTIL.getContextClassLoader();
                Class mapperClass = contextClassLoader.loadClass(userTypeMapping.getMapper());
                mapper = (Mapper) mapperClass.newInstance();
                javaType = contextClassLoader.loadClass(mappedTypeStr);
                if (cmpField.getSQLType() == null) {
                    JDBCMappingMetaData mappingMD = typeMapping.getTypeMappingMetaData(javaType);
                    sqlType = mappingMD.getSqlType();
                    jdbcType = mappingMD.getJdbcType();
                    paramSetter = mappingMD.getParamSetter();
                    resultReader = mappingMD.getResultReader();
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found for mapper: " + userTypeMapping.getMapper(), e);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate mapper: " + userTypeMapping.getMapper(), e);
            }
        }

        JDBCParameterSetter paramSetterImpl;
        if (paramSetter == null) {
            paramSetterImpl = JDBCUtil.getParameterSetter(jdbcType, javaType);
        } else {
            paramSetterImpl = (JDBCParameterSetter) newInstance(paramSetter);
        }

        JDBCResultSetReader resultReaderImpl;
        if (resultReader == null) {
            resultReaderImpl = JDBCUtil.getResultSetReader(jdbcType, javaType);
        } else {
            resultReaderImpl = (JDBCResultSetReader) newInstance(resultReader);
        }

        return new JDBCTypeSimple(
                columnName,
                javaType,
                jdbcType,
                sqlType,
                notNull,
                autoIncrement,
                mapper,
                paramSetterImpl,
                resultReaderImpl
        );
    }

    private JDBCTypeComplex createTypeComplex(JDBCCMPFieldMetaData cmpField) {
        // get the default properties for a field of its type
        JDBCTypeComplex type = (JDBCTypeComplex) complexTypes.get(cmpField.getFieldType());
        JDBCTypeComplexProperty[] defaultProperties = type.getProperties();

        // create a map of the overrides based on flat property name
        HashMap overrides = new HashMap();

        for (int i = 0; i < cmpField.getPropertyOverrides().size(); ++i) {
            JDBCCMPFieldPropertyMetaData p = (JDBCCMPFieldPropertyMetaData) cmpField.getPropertyOverrides().get(i);
            overrides.put(p.getPropertyName(), p);
        }

        // array that will hold the final properties after overrides
        JDBCTypeComplexProperty[] finalProperties = new JDBCTypeComplexProperty[defaultProperties.length];

        // override property default values
        for (int i = 0; i < defaultProperties.length; i++) {
            // pop off the override, if present
            JDBCCMPFieldPropertyMetaData override;
            override = (JDBCCMPFieldPropertyMetaData) overrides.remove(defaultProperties[i].getPropertyName());

            if (override == null) {
                finalProperties[i] = defaultProperties[i];
                finalProperties[i] = new JDBCTypeComplexProperty(
                        defaultProperties[i],
                        cmpField.getColumnName() + "_" +
                                defaultProperties[i].getColumnName(),
                        defaultProperties[i].getJDBCType(),
                        defaultProperties[i].getSQLType(),
                        cmpField.isNotNull() || defaultProperties[i].isNotNull());
            } else {
                // columnName
                String columnName = override.getColumnName();
                if (columnName == null) {
                    columnName = cmpField.getColumnName() + "_" + defaultProperties[i].getColumnName();
                }

                // sql and jdbc type
                String sqlType = override.getSQLType();
                int jdbcType;
                if (sqlType != null) {
                    jdbcType = override.getJDBCType();
                } else {
                    sqlType = defaultProperties[i].getSQLType();
                    jdbcType = defaultProperties[i].getJDBCType();
                }

                boolean notNull = cmpField.isNotNull() ||
                        override.isNotNull() ||
                        defaultProperties[i].isNotNull();

                finalProperties[i] = new JDBCTypeComplexProperty(
                        defaultProperties[i],
                        columnName,
                        jdbcType,
                        sqlType,
                        notNull);
            }
        }

        // did we find all overridden properties
        if (overrides.size() > 0) {
            String propertyName = (String) overrides.keySet().iterator().next();
            throw new EJBException("Property " + propertyName + " in field " +
                    cmpField.getFieldName() + " is not a property of value object " +
                    cmpField.getFieldType().getName());
        }

        // return the new complex type
        return new JDBCTypeComplex(finalProperties, cmpField.getFieldType());
    }

    private ArrayList createComplexProperties(
            JDBCValueClassMetaData valueClass,
            HashMap valueClassesByType,
            PropertyStack propertyStack) {

        ArrayList properties = new ArrayList();

        // add the properties each property to the list
        java.util.List valueClassProperties = valueClass.getProperties();
        for (int i = 0; i < valueClassProperties.size(); ++i) {
            JDBCValuePropertyMetaData propertyMetaData =
                    (JDBCValuePropertyMetaData) valueClassProperties.get(i);
            properties.addAll(createComplexProperties(propertyMetaData,
                    valueClassesByType, propertyStack));
        }
        return properties;
    }

    private ArrayList createComplexProperties(
            JDBCValuePropertyMetaData propertyMetaData,
            HashMap valueClassesByType,
            PropertyStack propertyStack) {

        // push my data onto the stack
        propertyStack.pushPropertyMetaData(propertyMetaData);

        ArrayList properties = new ArrayList();

        Class javaType = propertyMetaData.getPropertyType();
        if (!valueClassesByType.containsKey(javaType)) {

            // this property is a simple type
            // which makes this the end of the line for recursion
            String propertyName = propertyStack.getPropertyName();
            String columnName = propertyStack.getColumnName();

            String sqlType = propertyMetaData.getSqlType();
            int jdbcType;
            if (sqlType != null) {
                jdbcType = propertyMetaData.getJDBCType();
            } else {
                // get jdbcType and sqlType from typeMapping
                JDBCMappingMetaData typeMappingMD = typeMapping.getTypeMappingMetaData(javaType);
                sqlType = typeMappingMD.getSqlType();
                jdbcType = typeMappingMD.getJdbcType();
            }

            boolean notNull = propertyStack.isNotNull();

            Method[] getters = propertyStack.getGetters();
            Method[] setters = propertyStack.getSetters();

            properties.add(new JDBCTypeComplexProperty(
                    propertyName,
                    columnName,
                    javaType,
                    jdbcType,
                    sqlType,
                    notNull,
                    getters,
                    setters));

        } else {

            // this property is a value object, recurse
            JDBCValueClassMetaData valueClass =
                    (JDBCValueClassMetaData) valueClassesByType.get(javaType);
            properties.addAll(createComplexProperties(
                    valueClass,
                    valueClassesByType,
                    propertyStack));

        }

        // pop my data, back off
        propertyStack.popPropertyMetaData();

        return properties;
    }

    private static final class PropertyStack {
        final ArrayList properties = new ArrayList();
        final ArrayList propertyNames = new ArrayList();
        final ArrayList columnNames = new ArrayList();
        final ArrayList notNulls = new ArrayList();
        final ArrayList getters = new ArrayList();
        final ArrayList setters = new ArrayList();

        public PropertyStack() {
        }

        public void pushPropertyMetaData(
                JDBCValuePropertyMetaData propertyMetaData) {

            propertyNames.add(propertyMetaData.getPropertyName());
            columnNames.add(propertyMetaData.getColumnName());
            notNulls.add(new Boolean(propertyMetaData.isNotNull()));
            getters.add(propertyMetaData.getGetter());
            setters.add(propertyMetaData.getSetter());

            if (properties.contains(propertyMetaData)) {
                throw new EJBException("Circular reference discovered at " +
                        "property: " + getPropertyName());
            }
            properties.add(propertyMetaData);
        }

        public void popPropertyMetaData() {
            propertyNames.remove(propertyNames.size() - 1);
            columnNames.remove(columnNames.size() - 1);
            notNulls.remove(notNulls.size() - 1);
            getters.remove(getters.size() - 1);
            setters.remove(setters.size() - 1);

            properties.remove(properties.size() - 1);
        }

        public String getPropertyName() {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < propertyNames.size(); i++) {
                if (i > 0) {
                    buf.append(".");
                }
                buf.append((String) propertyNames.get(i));
            }
            return buf.toString();
        }

        public String getColumnName() {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    buf.append("_");
                }
                buf.append((String) columnNames.get(i));
            }
            return buf.toString();
        }

        public boolean isNotNull() {
            for (int i = 0; i < notNulls.size(); i++) {
                if (((Boolean) notNulls.get(i)).booleanValue()) {
                    return true;
                }
            }
            return false;
        }

        public Method[] getGetters() {
            return (Method[]) getters.toArray(new Method[getters.size()]);
        }

        public Method[] getSetters() {
            return (Method[]) setters.toArray(new Method[setters.size()]);
        }
    }


    private Object newInstance(String className) {
        Class clazz = loadClass(className);
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + className, e);
        }
    }

    private Class loadClass(String className) {
        try {
            final ClassLoader contextClassLoader = TCLAction.UTIL.getContextClassLoader();
            return contextClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + className, e);
        }
    }
}
