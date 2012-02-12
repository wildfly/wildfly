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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedQuery;
import org.jboss.metadata.ejb.spec.QueryMetaData;
import org.jboss.util.Classes;

/**
 * JDBCQueryMetaDataFactory constructs a JDBCQueryMetaData object based
 * on the query specifiection type.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public class JDBCQueryMetaDataFactory {
    public enum Type {RAW_SQL, JBOSS_QL, DYNAMIC_QL, DECLARED_QL, EJB_QL}

    private JDBCEntityMetaData entity;

    public JDBCQueryMetaDataFactory(JDBCEntityMetaData entity) {
        this.entity = entity;
    }

    public Map<Method, JDBCQueryMetaData> createJDBCQueryMetaData(QueryMetaData queryData) {
        List<Method> methods = getQueryMethods(queryData);
        Map<Method, JDBCQueryMetaData> queries = new HashMap<Method, JDBCQueryMetaData>(methods.size());
        for (Method method : methods) {
            queries.put(method, new JDBCQlQueryMetaData(queryData, method, entity.getQlCompiler(), false));
        }
        return queries;
    }

    public JDBCQueryMetaData createJDBCQueryMetaData(JDBCQueryMetaData jdbcQueryMetaData, JDBCReadAheadMetaData readAhead, Class<?> qlCompiler) {
        // RAW-SQL
        if (jdbcQueryMetaData instanceof JDBCRawSqlQueryMetaData) {
            return new JDBCRawSqlQueryMetaData(jdbcQueryMetaData.getMethod(), qlCompiler, false);
        }

        // JBOSS-QL
        if (jdbcQueryMetaData instanceof JDBCJBossQLQueryMetaData) {
            return new JDBCJBossQLQueryMetaData(
                    (JDBCJBossQLQueryMetaData) jdbcQueryMetaData,
                    readAhead, null, qlCompiler, false
            );
        }

        // DYNAMIC-SQL
        if (jdbcQueryMetaData instanceof JDBCDynamicQLQueryMetaData) {
            return new JDBCDynamicQLQueryMetaData(
                    (JDBCDynamicQLQueryMetaData) jdbcQueryMetaData,
                    readAhead, qlCompiler, false
            );
        }

        // DECLARED-SQL
        if (jdbcQueryMetaData instanceof JDBCDeclaredQueryMetaData) {
            return new JDBCDeclaredQueryMetaData(
                    (JDBCDeclaredQueryMetaData) jdbcQueryMetaData,
                    readAhead, qlCompiler, false
            );
        }

        // EJB-QL: default
        if (jdbcQueryMetaData instanceof JDBCQlQueryMetaData) {
            return new JDBCQlQueryMetaData(
                    (JDBCQlQueryMetaData) jdbcQueryMetaData,
                    readAhead, qlCompiler, false
            );
        }

        throw new RuntimeException(
                "Error in query specification for method " +
                        jdbcQueryMetaData.getMethod().getName()
        );
    }

    public List<JDBCQueryMetaData> createJDBCQueryMetaData(final ParsedQuery parsedQuery) {
        final Class<?>[] parameters;
        try {
            parameters = Classes.convertToJavaClasses(parsedQuery.getMethodParams().iterator(), entity.getJDBCApplication().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to convert method params to class instances: " + parsedQuery.getMethodParams());
        }
        final List<Method> methods = getQueryMethods(parsedQuery.getMethodName(), parameters);

        final Class<?> qlCompiler = parsedQuery.getQlCompiler() != null ? parsedQuery.getQlCompiler() : entity.getQlCompiler();
        final JDBCReadAheadMetaData readAhead;
        if(parsedQuery.getReadAheadMetaData() != null) {
            readAhead = new JDBCReadAheadMetaData(parsedQuery.getReadAheadMetaData(), entity.getReadAhead());
        } else {
            readAhead = entity.getReadAhead();
        }
        final List<JDBCQueryMetaData> built = new ArrayList<JDBCQueryMetaData>(methods.size());
        for (Method method : methods) {
            final JDBCQueryMetaData defaultValue = entity.getQueryMetaDataForMethod(method);
            final JDBCQueryMetaData queryMetaData;
            final boolean isResultTypeMappingLocal = defaultValue != null && defaultValue.isResultTypeMappingLocal();
            switch (parsedQuery.getType()) {
                case RAW_SQL: {
                    queryMetaData = new JDBCRawSqlQueryMetaData(method, qlCompiler, parsedQuery.isLazyResultsetLoading());
                    break;
                }
                case JBOSS_QL: {
                    queryMetaData = new JDBCJBossQLQueryMetaData(isResultTypeMappingLocal, parsedQuery.getQuery(), method, readAhead, qlCompiler, parsedQuery.isLazyResultsetLoading());
                    break;
                }
                case DYNAMIC_QL: {
                    queryMetaData = new JDBCDynamicQLQueryMetaData(isResultTypeMappingLocal, method, readAhead, qlCompiler, parsedQuery.isLazyResultsetLoading());
                    break;
                }
                case DECLARED_QL: {
                    queryMetaData = new JDBCDeclaredQueryMetaData(isResultTypeMappingLocal, method, readAhead, qlCompiler, parsedQuery.isLazyResultsetLoading(), parsedQuery.getDeclaredParts());
                    break;
                }
                default: {
                    if(defaultValue != null && defaultValue instanceof JDBCQlQueryMetaData) {
                        queryMetaData = new JDBCQlQueryMetaData((JDBCQlQueryMetaData) defaultValue, readAhead, qlCompiler, false);
                    } else {
                        throw new RuntimeException("Error in query specification for method " + method.getName());
                    }

                    break;
                }
            }
            built.add(queryMetaData);
        }
        return built;
    }

    private List<Method> getQueryMethods(QueryMetaData queryData) {
        String methodName = queryData.getQueryMethod().getMethodName();
        try {
            Class<?>[] parameters = Classes.convertToJavaClasses(queryData.getQueryMethod().getMethodParams().iterator(), entity.getJDBCApplication().getClassLoader());
            return getQueryMethods(methodName, parameters);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe.getMessage());
        }
    }

    private List<Method> getQueryMethods(String methodName, Class<?>[] parameters) {
        // find the query and load the xml
        List<Method> methods = new ArrayList<Method>(2);
        if (methodName.startsWith("ejbSelect")) {
            // bean method
            Method method = getQueryMethod(methodName, parameters, entity.getEntityClass());
            if (method != null) {
                methods.add(method);
            }
        } else {
            // remote home
            Class<?> homeClass = entity.getHomeClass();
            if (homeClass != null) {
                Method method = getQueryMethod(methodName, parameters, homeClass);
                if (method != null) {
                    methods.add(method);
                }
            }
            // local home
            Class<?> localHomeClass = entity.getLocalHomeClass();
            if (localHomeClass != null) {
                Method method = getQueryMethod(methodName, parameters, localHomeClass);
                if (method != null) {
                    methods.add(method);
                }
            }
        }

        if (methods.size() == 0) {
            StringBuffer sb = new StringBuffer(300);
            sb.append("Query method not found: ")
                    .append(methodName).append('(');
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(parameters[i].getName());
            }
            sb.append(')');
            throw new RuntimeException(sb.toString());
        }
        return methods;
    }

    private static Method getQueryMethod(String queryName, Class<?>[] parameters, Class<?> clazz) {
        try {
            Method method = clazz.getMethod(queryName, parameters);
            // is the method abstract?
            // (remember interface methods are always abstract)
            if (Modifier.isAbstract(method.getModifiers())) {
                return method;
            }
        } catch (NoSuchMethodException e) {
            // that's cool
        }
        return null;
    }
}
