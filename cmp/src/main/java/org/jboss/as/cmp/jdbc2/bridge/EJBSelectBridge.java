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
package org.jboss.as.cmp.jdbc2.bridge;

import java.lang.reflect.Method;
import java.util.Collection;
import javax.ejb.FinderException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.bridge.SelectorBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;
import org.jboss.as.cmp.jdbc2.QueryCommand;
import org.jboss.as.cmp.jdbc2.schema.Schema;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class EJBSelectBridge implements SelectorBridge {
    private static final byte SINGLE = 0;
    private static final byte COLLECTION = 2;

    private final JDBCQueryMetaData metadata;
    private final QueryCommand command;
    private final byte returnType;
    private final Schema schema;
    private boolean syncBeforeSelect;
    private TransactionManager tm;

    public EJBSelectBridge(final JDBCStoreManager2 manager, CmpEntityBeanComponent component, Schema schema, JDBCQueryMetaData metadata, QueryCommand command) {
        this.schema = schema;
        this.metadata = metadata;
        this.command = command;

        Class type = metadata.getMethod().getReturnType();
        if (Collection.class.isAssignableFrom(type)) {
            returnType = COLLECTION;
        } else {
            returnType = SINGLE;
        }

        tm = component.getTransactionManager();
        syncBeforeSelect = !manager.getCmpConfig().isSyncOnCommitOnly();
    }

    // BridgeInvoker implementation

    public Object invoke(CmpEntityBeanContext instance, Method method, Object[] args)
            throws Exception {
        Transaction tx = (instance != null ? instance.getTransaction() : tm.getTransaction());

        if (syncBeforeSelect) {
            instance.getComponent().synchronizeEntitiesWithinTransaction(tx);
        }

        return execute(instance, args);
    }

    // SelectorBridge implementation

    public String getSelectorName() {
        return metadata.getMethod().getName();
    }

    public Method getMethod() {
        return metadata.getMethod();
    }

    public Object execute(CmpEntityBeanContext ctx, Object[] args) throws FinderException {
        JDBCStoreManager2 manager = command.getStoreManager();
        final CmpEntityBeanComponent selectedComponent = manager.getComponent();
        JDBCQueryCommand.EntityProxyFactory factory = new JDBCQueryCommand.EntityProxyFactory() {
            public Object getEntityObject(Object primaryKey) {
                return metadata.isResultTypeMappingLocal() && selectedComponent.getLocalHomeClass() != null ?
                        selectedComponent.getEJBLocalObject(primaryKey) : selectedComponent.getEJBObject(primaryKey);
            }
        };
        Object result;
        switch (returnType) {
            case SINGLE:
                result = command.fetchOne(schema, args, factory);
                if (result == null && getMethod().getReturnType().isPrimitive()) {
                    throw CmpMessages.MESSAGES.cannotReturnNullForPrimitive(getMethod().getReturnType().getName());
                }
                break;
            case COLLECTION:
                result = command.fetchCollection(schema, args, factory);
                break;
            default:
                throw CmpMessages.MESSAGES.unexpectedReturnType(returnType);
        }
        return result;
    }
}
