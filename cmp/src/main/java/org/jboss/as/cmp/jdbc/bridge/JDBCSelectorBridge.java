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
package org.jboss.as.cmp.jdbc.bridge;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.bridge.SelectorBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;

/**
 * JDBCSelectorBridge represents one ejbSelect method.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each entity bean ejbSelect method.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public class JDBCSelectorBridge implements SelectorBridge {
    private final JDBCQueryMetaData queryMetaData;
    private final JDBCStoreManager manager;
    private TransactionManager tm;
    private boolean syncBeforeSelect;

    public JDBCSelectorBridge(JDBCStoreManager manager, JDBCQueryMetaData queryMetaData) {
        this.queryMetaData = queryMetaData;
        this.manager = manager;

        CmpEntityBeanComponent component = manager.getComponent();
        tm = component.getTransactionManager();
        syncBeforeSelect = !manager.getCmpConfig().isSyncOnCommitOnly();
    }

    // BridgeInvoker implementation

    public Object invoke(CmpEntityBeanContext ctx, Method method, Object[] args) throws Exception {
        Transaction tx = (ctx != null ? ctx.getTransaction() : tm.getTransaction());

        if (syncBeforeSelect) {
            manager.getComponent().synchronizeEntitiesWithinTransaction(tx);
        }

        return execute(ctx, args);
    }

    // SelectorBridge implementation

    public String getSelectorName() {
        return queryMetaData.getMethod().getName();
    }

    public Method getMethod() {
        return queryMetaData.getMethod();
    }

    private Class getReturnType() {
        return queryMetaData.getMethod().getReturnType();
    }

    public Object execute(CmpEntityBeanContext ctx, Object[] args) throws FinderException {
        Collection retVal;
        Method method = getMethod();
        try {
            JDBCQueryCommand query = manager.getQueryManager().getQueryCommand(method);
            final CmpEntityBeanComponent selectedComponent = query.getSelectManager().getComponent();
            JDBCQueryCommand.EntityProxyFactory factory = new JDBCQueryCommand.EntityProxyFactory() {
                public Object getEntityObject(Object primaryKey) {
                    return queryMetaData.isResultTypeMappingLocal() && selectedComponent.getLocalHomeClass() != null ?
                            selectedComponent.getEJBLocalObject(primaryKey) : selectedComponent.getEJBObject(primaryKey);
                }
            };
            retVal = query.execute(method, args, null, factory);
        } catch (FinderException e) {
            throw e;
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException("Error in " + getSelectorName(), e);
        }

        if (!Collection.class.isAssignableFrom(getReturnType())) {
            // single object
            if (retVal.size() == 0) {
                throw new ObjectNotFoundException();
            }
            if (retVal.size() > 1) {
                throw new FinderException(getSelectorName() +
                        " returned " + retVal.size() + " objects");
            }

            Object o = retVal.iterator().next();
            if (o == null && method.getReturnType().isPrimitive()) {
                throw new FinderException(
                        "Cannot return null as a value of primitive type " + method.getReturnType().getName()
                );
            }

            return o;
        } else {
            // collection or set
            if (Set.class.isAssignableFrom(getReturnType())) {
                return new HashSet(retVal);
            } else {
                return retVal;
            }
        }
    }
}
