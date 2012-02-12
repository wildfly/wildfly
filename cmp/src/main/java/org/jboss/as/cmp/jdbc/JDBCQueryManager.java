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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.ejb.FinderException;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCAutomaticQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCDeclaredQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCDynamicQLQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCJBossQLQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQlQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.logging.Logger;

/**
 * Maintains a map from a query method to query command.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCQueryManager {
    private static final String FIND_BY_PK = "findByPrimaryKey";
    private static final String EJB_FIND = "ejbFind";
    private static final String FIND_ALL = "findAll";
    private static final String FIND_BY = "findBy";

    private final Map knownQueries = new HashMap();
    private final JDBCStoreManager manager;

    public JDBCQueryManager(JDBCStoreManager manager) {
        this.manager = manager;
    }

    public static QLCompiler getInstance(Class impl, Catalog catalog) {
        if (impl == null) {
            throw new RuntimeException("ql-compiler is not specified.");
        }

        final Constructor constructor;
        try {
            constructor = impl.getConstructor(new Class[]{Catalog.class});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Compiler class does not have a constructor which takes " + Catalog.class.getName());
        }

        try {
            return (QLCompiler) constructor.newInstance(new Object[]{catalog});
        } catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of " + impl.getName() + ": " + e.getMessage(), e);
        }
    }

    public JDBCQueryCommand getQueryCommand(Method queryMethod)
            throws FinderException {
        JDBCQueryCommand queryCommand = (JDBCQueryCommand) knownQueries.get(queryMethod);

        if (queryCommand == null) {
            throw new FinderException("Unknown query: " + queryMethod);
        }
        return queryCommand;
    }

    public void start() {
        Logger log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());

        JDBCCommandFactory factory = manager.getCommandFactory();

        Class homeClass = manager.getComponent().getHomeClass();
        Class localHomeClass = manager.getComponent().getLocalHomeClass();

        //
        // findByPrimaryKey
        //
        JDBCEntityBridge entity = (JDBCEntityBridge) manager.getEntityBridge();
        if (homeClass != null) {
            try {
                // try to get the finder method on the home interface
                Method method = homeClass.getMethod(FIND_BY_PK, new Class[]{entity.getPrimaryKeyClass()});

                JDBCQueryMetaData findByPKMD = manager.getMetaData().getQueryMetaDataForMethod(method);
                JDBCReadAheadMetaData readAhead = (findByPKMD == null ?
                        entity.getMetaData().getReadAhead() : findByPKMD.getReadAhead());

                // got it add it to known finders
                JDBCQueryMetaData q = new JDBCAutomaticQueryMetaData(
                        method,
                        readAhead,
                        entity.getMetaData().getQlCompiler(),
                        false
                );
                knownQueries.put(method, factory.createFindByPrimaryKeyQuery(q));

                if (log.isDebugEnabled())
                    log.debug("Added findByPrimaryKey query command for home interface");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Home interface does not have a findByPrimaryKey method");
            }
        }

        if (localHomeClass != null) {

            Method method;
            try {
                // try to get the finder method on the local home interface
                method = localHomeClass.getMethod(FIND_BY_PK, new Class[]{entity.getPrimaryKeyClass()});
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Local home interface does " +
                        "not have the method findByPrimaryKey(" +
                        entity.getPrimaryKeyClass().getName() + ")");
            }

            // got it add it to known finders
            JDBCQueryMetaData findByPKMD = manager.getMetaData().getQueryMetaDataForMethod(method);
            JDBCReadAheadMetaData readAhead = (findByPKMD == null ?
                    entity.getMetaData().getReadAhead() : findByPKMD.getReadAhead());
            JDBCQueryMetaData q = new JDBCAutomaticQueryMetaData(method, readAhead, entity.getMetaData().getQlCompiler(), false);
            knownQueries.put(method, factory.createFindByPrimaryKeyQuery(q));

            if (log.isDebugEnabled())
                log.debug("Added findByPrimaryKey query command for local home interface");
        }

        //
        // Custom finders - Overrides defined and automatic finders.
        //
        Class ejbClass = manager.getMetaData().getEntityClass();

        Method[] customMethods = ejbClass.getMethods();
        for (int i = 0; i < customMethods.length; i++) {
            Method m = customMethods[i];
            String methodName = m.getName();
            if (methodName.startsWith(EJB_FIND)) {
                String interfaceName = 'f' + methodName.substring(4);

                if (homeClass != null) {
                    try {
                        // try to get the finder method on the home interface
                        Method interfaceMethod = homeClass.getMethod(
                                interfaceName,
                                m.getParameterTypes());

                        // got it add it to known finders
                        knownQueries.put(interfaceMethod, new JDBCCustomFinderQuery(manager, m));

                        if (log.isDebugEnabled())
                            log.debug("Added custom finder " + methodName + " on home interface");
                    } catch (NoSuchMethodException e) {
                        // this is ok method may not be defined on this interface
                    }
                }

                if (localHomeClass != null) {
                    try {
                        // try to get the finder method on the local home interface
                        Method interfaceMethod = localHomeClass.getMethod(
                                interfaceName,
                                m.getParameterTypes());

                        // got it add it to known finders
                        knownQueries.put(interfaceMethod, new JDBCCustomFinderQuery(manager, m));

                        if (log.isDebugEnabled())
                            log.debug("Added custom finder " + methodName + " on local home interface");
                    } catch (NoSuchMethodException e) {
                        // this is ok method may not be defined on this interface
                    }
                }
            }
        }

        //
        // Defined finders - Overrides automatic finders.
        //
        Iterator definedFinders = manager.getMetaData().getQueries().iterator();
        while (definedFinders.hasNext()) {
            JDBCQueryMetaData q = (JDBCQueryMetaData) definedFinders.next();

            if (!knownQueries.containsKey(q.getMethod())) {
                if (q instanceof JDBCJBossQLQueryMetaData) {
                    knownQueries.put(q.getMethod(), factory.createJBossQLQuery(q));

                } else if (q instanceof JDBCDynamicQLQueryMetaData) {
                    knownQueries.put(q.getMethod(), factory.createDynamicQLQuery(q));

                } else if (q instanceof JDBCDeclaredQueryMetaData) {
                    knownQueries.put(q.getMethod(), factory.createDeclaredSQLQuery(q));

                } else if (q instanceof JDBCQlQueryMetaData) {
                    knownQueries.put(q.getMethod(), factory.createEJBQLQuery(q));
                }
            }
        }

        //
        // Automatic finders - The last resort
        //
        if (homeClass != null) {
            addAutomaticFinders(manager, homeClass.getMethods(), log);
        }

        if (localHomeClass != null) {
            addAutomaticFinders(manager, localHomeClass.getMethods(), log);
        }
    }

    public void clear() {
        this.knownQueries.clear();
    }

    private void addAutomaticFinders(
            JDBCStoreManager manager,
            Method[] homeMethods,
            Logger log) {

        JDBCCommandFactory factory = manager.getCommandFactory();
        JDBCEntityBridge entity = (JDBCEntityBridge) manager.getEntityBridge();
        for (int i = 0; i < homeMethods.length; i++) {
            Method method = homeMethods[i];

            if (!knownQueries.containsKey(method)) {
                String name = method.getName();
                if (name.equals(FIND_ALL)) {
                    JDBCQueryMetaData q = new JDBCAutomaticQueryMetaData(
                            method,
                            entity.getMetaData().getReadAhead(),
                            entity.getMetaData().getQlCompiler(), false
                    );
                    knownQueries.put(method, factory.createFindAllQuery(q));
                } else if (name.startsWith(FIND_BY) && !name.equals(FIND_BY_PK)) {
                    try {
                        JDBCQueryMetaData q = new JDBCAutomaticQueryMetaData(
                                method,
                                entity.getMetaData().getReadAhead(),
                                entity.getMetaData().getQlCompiler(), false);
                        knownQueries.put(method, factory.createFindByQuery(q));
                    } catch (IllegalArgumentException e) {
                        log.debug("Could not create the finder " + name +
                                ", because no matching CMP field was found.");
                    }
                }
            }
        }
    }
}
