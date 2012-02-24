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
package org.jboss.as.cmp.jdbc2;


import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCJBossQLQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQlQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCDeclaredQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCDynamicQLQueryMetaData;

import javax.ejb.FinderException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class QueryFactory {
    private final Map queriesByMethod = new HashMap();
    private final JDBCEntityBridge2 entity;

    public QueryFactory(JDBCEntityBridge2 entity) {
        this.entity = entity;
    }

    public QueryCommand getQueryCommand(Method queryMethod) throws FinderException {
        QueryCommand queryCommand = (QueryCommand) queriesByMethod.get(queryMethod);
        if (queryCommand == null) {
            throw CmpMessages.MESSAGES.unknownQueryMethod(queryMethod);
        }
        return queryCommand;
    }

    public void init() {
        Method findByPkMethod;
        Class home = entity.getHomeClass();
        if (home != null) {
            try {
                findByPkMethod = home.getMethod("findByPrimaryKey", new Class[]{entity.getPrimaryKeyClass()});
            } catch (NoSuchMethodException e) {
                throw CmpMessages.MESSAGES.homeInterfaceNoPKMethod(home.getClass().getName(), entity.getPrimaryKeyClass().getName());
            }

            FindByPrimaryKeyCommand findByPk = new FindByPrimaryKeyCommand(entity);
            queriesByMethod.put(findByPkMethod, findByPk);
        }

        Class local = entity.getLocalHomeClass();
        if (local != null) {
            try {
                findByPkMethod = local.getMethod("findByPrimaryKey", new Class[]{entity.getPrimaryKeyClass()});
            } catch (NoSuchMethodException e) {
                throw CmpMessages.MESSAGES.localHomeInterfaceNoPKMethod(local.getClass().getName(), entity.getPrimaryKeyClass().getName());
            }

            FindByPrimaryKeyCommand findByPk = new FindByPrimaryKeyCommand(entity);
            queriesByMethod.put(findByPkMethod, findByPk);
        }

        //
        // Defined finders - Overrides automatic finders.
        //
        Iterator definedFinders = entity.getMetaData().getQueries().iterator();
        while (definedFinders.hasNext()) {
            JDBCQueryMetaData q = (JDBCQueryMetaData) definedFinders.next();

            if (!queriesByMethod.containsKey(q.getMethod())) {
                if (q instanceof JDBCJBossQLQueryMetaData) {
                    QueryCommand queryCommand = new JBossQLQueryCommand(entity, (JDBCJBossQLQueryMetaData) q);
                    queriesByMethod.put(q.getMethod(), queryCommand);
                } else if (q instanceof JDBCQlQueryMetaData) {
                    QueryCommand queryCommand = new EJBQLQueryCommand(entity, (JDBCQlQueryMetaData) q);
                    queriesByMethod.put(q.getMethod(), queryCommand);
                } else if (q instanceof JDBCDeclaredQueryMetaData) {
                    QueryCommand queryCommand = new DeclaredSQLQueryCommand(entity, (JDBCDeclaredQueryMetaData) q);
                    queriesByMethod.put(q.getMethod(), queryCommand);
                } else if (q instanceof JDBCDynamicQLQueryMetaData) {
                    QueryCommand queryCommand = new DynamicQueryCommand(entity, (JDBCDynamicQLQueryMetaData) q);
                    queriesByMethod.put(q.getMethod(), queryCommand);
                } else {
                    throw CmpMessages.MESSAGES.unsupportedQueryMetadata(q.getMethod().getName(), q);
                }
            }
        }
    }
}
