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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky and others</a>
 */

public final class QueryParameter {
    public static List createParameters(int argNum, JDBCFieldBridge field) {
        List parameters;
        JDBCType type = field.getJDBCType();
        if (type instanceof JDBCTypeComplex) {
            JDBCTypeComplexProperty[] props = ((JDBCTypeComplex) type).getProperties();
            parameters = new ArrayList(props.length);
            for (int i = 0; i < props.length; i++) {
                QueryParameter param = new QueryParameter(
                        argNum,
                        false,
                        null,
                        props[i],
                        props[i].getJDBCType());
                parameters.add(param);
            }
        } else {
            QueryParameter param = new QueryParameter(argNum, type);
            parameters = Collections.singletonList(param);
        }
        return parameters;
    }

    public static List createParameters(int argNum, JDBCAbstractEntityBridge entity) {
        List parameters = new ArrayList();
        JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
        for (int i = 0; i < pkFields.length; ++i) {
            JDBCFieldBridge pkField = pkFields[i];

            JDBCType type = pkField.getJDBCType();
            if (type instanceof JDBCTypeComplex) {
                JDBCTypeComplexProperty[] props =
                        ((JDBCTypeComplex) type).getProperties();
                for (int j = 0; j < props.length; j++) {
                    QueryParameter param = new QueryParameter(
                            argNum,
                            false,
                            pkField,
                            props[j],
                            props[j].getJDBCType());
                    parameters.add(param);
                }
            } else {
                QueryParameter param = new QueryParameter(
                        argNum,
                        false,
                        pkField,
                        null,
                        type.getJDBCTypes()[0]);
                param.type = type;
                parameters.add(param);
            }
        }
        return parameters;
    }

    public static List createPrimaryKeyParameters(int argNum, JDBCAbstractEntityBridge entity) {
        List parameters = new ArrayList();
        JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
        for (int i = 0; i < pkFields.length; ++i) {
            JDBCFieldBridge pkField = pkFields[i];

            JDBCType type = pkField.getJDBCType();
            if (type instanceof JDBCTypeComplex) {
                JDBCTypeComplexProperty[] props = ((JDBCTypeComplex) type).getProperties();
                for (int j = 0; j < props.length; j++) {
                    QueryParameter param = new QueryParameter(
                            argNum,
                            true,
                            pkField,
                            props[j],
                            props[j].getJDBCType());
                    parameters.add(param);
                }
            } else {
                QueryParameter param = new QueryParameter(
                        argNum,
                        true,
                        pkField,
                        null,
                        type.getJDBCTypes()[0]);
                param.type = type;
                parameters.add(param);
            }
        }
        return parameters;
    }

    private int argNum;
    private final boolean isPrimaryKeyParameter;
    private JDBCFieldBridge field;
    private JDBCTypeComplexProperty property;
    private String parameterString;

    private int jdbcType;
    private JDBCType type;

    public QueryParameter(JDBCEntityPersistenceStore manager,
                          Method method,
                          String parameterString) {

        // Method parameter will never be a primary key object, but always
        // a complete entity.
        this.isPrimaryKeyParameter = false;

        this.parameterString = parameterString;

        if (parameterString == null || parameterString.length() == 0) {
            throw MESSAGES.parameterStringIsEmpty();
        }

        StringTokenizer tok = new StringTokenizer(parameterString, ".");

        // get the argument number
        try {
            argNum = Integer.parseInt(tok.nextToken());
        } catch (NumberFormatException e) {
            throw MESSAGES.parameterMustBeginWithNumber();
        }

        // get the argument type
        if (argNum > method.getParameterTypes().length) {
            throw CmpMessages.MESSAGES.invalidParameterInQueryMethod(argNum, method.getParameterTypes().length);
        }
        Class argType = method.getParameterTypes()[argNum];

        // get the jdbc type object
        JDBCType type;

        // if this is an entity parameter
        if (EJBObject.class.isAssignableFrom(argType) ||
                EJBLocalObject.class.isAssignableFrom(argType)) {
            // get the field name
            // check more tokens
            if (!tok.hasMoreTokens()) {
                throw MESSAGES.fieldNameMustBeProvided();
            }
            String fieldName = tok.nextToken();

            // get the field from the entity
            field = getCMPField(manager, argType, fieldName);
            if (!field.isPrimaryKeyMember()) {
                throw MESSAGES.fieldMustBePrimaryKey();
            }

            // get the jdbc type object
            type = field.getJDBCType();
        } else {
            // get jdbc type from type manager
            type = manager.getJDBCTypeFactory().getJDBCType(argType);
        }

        if (type instanceof JDBCTypeSimple) {
            if (tok.hasMoreTokens()) {
                throw CmpMessages.MESSAGES.typePropertiesNotAllowed();
            }
            jdbcType = type.getJDBCTypes()[0];
            this.type = type;
        } else {
            if (!tok.hasMoreTokens()) {
                throw CmpMessages.MESSAGES.typePropertyRequired();
            }

            // build the propertyName
            StringBuffer propertyName = new StringBuffer(parameterString.length());
            propertyName.append(tok.nextToken());
            while (tok.hasMoreTokens()) {
                propertyName.append('.').append(tok.nextToken());
            }
            property = ((JDBCTypeComplex) type).getProperty(propertyName.toString());
            jdbcType = property.getJDBCType();
        }
    }

    public QueryParameter(int argNum, JDBCType type) {
        this.argNum = argNum;
        this.type = type;
        this.jdbcType = type.getJDBCTypes()[0];
        this.isPrimaryKeyParameter = false;
        initToString();
    }

    public QueryParameter(
            int argNum,
            boolean isPrimaryKeyParameter,
            JDBCFieldBridge field,
            JDBCTypeComplexProperty property,
            int jdbcType) {

        this.argNum = argNum;
        this.isPrimaryKeyParameter = isPrimaryKeyParameter;
        this.field = field;
        this.property = property;
        this.jdbcType = jdbcType;

        initToString();
    }

    private void initToString() {
        StringBuffer parameterBuf = new StringBuffer();
        parameterBuf.append(argNum);
        if (field != null) {
            parameterBuf.append('.').append(field.getFieldName());
        }
        if (property != null) {
            parameterBuf.append('.').append(property.getPropertyName());
        }
        parameterString = parameterBuf.toString();
    }

    public void set(Logger log, PreparedStatement ps, int index, Object[] args)
            throws Exception {
        Object arg = args[argNum];
        JDBCParameterSetter param;
        if (field != null) {
            if (!isPrimaryKeyParameter) {
                if (arg instanceof EJBObject) {
                    arg = ((EJBObject) arg).getPrimaryKey();
                } else if (arg instanceof EJBLocalObject) {
                    arg = ((EJBLocalObject) arg).getPrimaryKey();
                } else {
                    throw CmpMessages.MESSAGES.expectedEjbObject(arg.getClass().getName());
                }
            }
            arg = field.getPrimaryKeyValue(arg);

            // use mapper
            final JDBCType jdbcType = field.getJDBCType();
            arg = jdbcType.getColumnValue(0, arg);
            param = jdbcType.getParameterSetter()[0];
        } else if (property != null) {
            arg = property.getColumnValue(arg);
            param = property.getParameterSetter();
        } else {
            if (type != null) {
                arg = type.getColumnValue(0, arg);
                param = type.getParameterSetter()[0];
            } else {
                param = JDBCUtil.getParameterSetter(jdbcType, arg == null ? null : arg.getClass());
            }
        }
        param.set(ps, index, jdbcType, arg, log);
        //JDBCUtil.setParameter(log, ps, index, jdbcType, arg);
    }

    private static JDBCFieldBridge getCMPField(
            JDBCEntityPersistenceStore manager,
            Class intf,
            String fieldName) {
        Catalog catalog = manager.getCatalog();
        JDBCAbstractEntityBridge entityBridge = (JDBCAbstractEntityBridge) catalog.getEntityByInterface(intf);
        if (entityBridge == null) {
            throw CmpMessages.MESSAGES.entityNotFoundInCatalog(intf.getName());
        }

        JDBCFieldBridge cmpField = (JDBCFieldBridge) entityBridge.getFieldByName(fieldName);
        if (cmpField == null) {
            throw CmpMessages.MESSAGES.cmpFieldNotFound(fieldName, entityBridge.getEntityName());
        }
        return cmpField;
    }

    public String toString() {
        return parameterString;
    }
}
