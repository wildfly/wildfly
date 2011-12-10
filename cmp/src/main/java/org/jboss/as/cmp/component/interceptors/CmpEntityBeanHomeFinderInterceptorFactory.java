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

package org.jboss.as.cmp.component.interceptors;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import javax.ejb.ObjectNotFoundException;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanHomeFinderInterceptorFactory;
import org.jboss.invocation.InterceptorContext;

/**
 * @author John Bailey
 */
public class CmpEntityBeanHomeFinderInterceptorFactory extends EntityBeanHomeFinderInterceptorFactory {
    private final Method finderMethod;
    private final boolean localHome;

    public CmpEntityBeanHomeFinderInterceptorFactory(final Method finderMethod, final boolean localHome) {
        super(finderMethod);
        this.finderMethod = finderMethod;
        this.localHome = localHome;
    }


    protected Object invokeFind(final InterceptorContext context, final EntityBeanComponentInstance instance) throws Exception {
        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final CmpEntityBeanComponent cmpComponent = cmpInstance.getComponent();
        try {
            cmpComponent.getComponentClass().getDeclaredMethod(finderMethod.getName(), finderMethod.getParameterTypes());
            return super.invokeFind(context, instance);
        } catch (NoSuchMethodException ignored) {
        }

        final JDBCEntityPersistenceStore store = cmpComponent.getStoreManager();

        final CmpEntityBeanContext entityContext = cmpInstance.getEjbContext();

        // as per the spec 9.6.4, entities must be synchronized with the datastore when an ejbFind<METHOD> is called.
        if (!store.getCmpConfig().isSyncOnCommitOnly()) {
            cmpComponent.synchronizeEntitiesWithinTransaction(entityContext.getTransaction());
        }

        final JDBCQueryCommand.EntityProxyFactory factory = new JDBCQueryCommand.EntityProxyFactory() {
            public Object getEntityObject(final Object primaryKey) {
                return localHome ? cmpComponent.getEJBLocalObject(primaryKey) : cmpComponent.getEJBObject(primaryKey);
            }
        };

        if (getReturnType() == ReturnType.SINGLE) {
            return store.findEntity(context.getMethod(), context.getParameters(), entityContext, factory);
        } else {
            return store.findEntities(context.getMethod(), context.getParameters(), entityContext, factory);
        }
    }

    protected Object prepareResults(final InterceptorContext context, final Object result, final EntityBeanComponent component) throws ObjectNotFoundException {
        switch (getReturnType()) {
            case COLLECTION: {
                return result;
            }
            case ENUMERATION: {
                Collection<Object> entities = (Collection<Object>) result;
                final Iterator<Object> iterator = entities.iterator();
                return new Enumeration<Object>() {
                    public boolean hasMoreElements() {
                        return iterator.hasNext();
                    }

                    public Object nextElement() {
                        return iterator.next();
                    }
                };
            }
            default: {
                if (result == null) {
                    throw new ObjectNotFoundException("Could not find entity from " + finderMethod + " with params " + Arrays.toString(context.getParameters()));
                }
                return result;
            }
        }
    }
}
