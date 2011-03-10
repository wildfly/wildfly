/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.AbstractComponentConfiguration;
import org.jboss.as.ejb3.EJBUtilities;
import org.jboss.ejb3.tx2.spi.TransactionalComponent;
import org.jboss.logging.Logger;

import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponent extends AbstractComponent implements TransactionalComponent {
    private static Logger log = Logger.getLogger(EJBComponent.class);

    private final ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> txAttrs;

    private final EJBUtilities utilities;

    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     */
    protected EJBComponent(final AbstractComponentConfiguration configuration) {
        super(configuration);

        this.utilities = configuration.getInjectionValue(EJBUtilities.SERVICE_NAME, EJBUtilities.class);

        // slurp some memory
        txAttrs = ((EJBComponentConfiguration) configuration).getTxAttrs();
    }

    @Override
    public ApplicationException getApplicationException(Class<?> exceptionClass) {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.EJBComponent.getApplicationException");
    }

    @Deprecated
    public TransactionAttributeType getTransactionAttributeType(Method method) {
        log.warn("EJBTHREE-2120: deprecated getTransactionAttributeType method called (dev problem)");
        return getTransactionAttributeType(MethodIntf.BEAN, method);
    }

    public TransactionAttributeType getTransactionAttributeType(MethodIntf methodIntf, Method method) {
        ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>> perMethodIntf = txAttrs.get(methodIntf);
        if (perMethodIntf == null)
            throw new IllegalStateException("Can't find tx attrs for " + methodIntf);
        ConcurrentMap<ArrayKey, TransactionAttributeType> perMethod = perMethodIntf.get(method.getName());
        if (perMethod == null)
            throw new IllegalStateException("Can't find tx attrs for method name " + method.getName() + " via " + methodIntf);
        TransactionAttributeType txAttr = perMethod.get(new ArrayKey(method.getParameterTypes()));
        if (txAttr == null)
            throw new IllegalStateException("Can't find tx attr for method " + method + " via " + methodIntf);
        return txAttr;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return utilities.getTransactionManager();
    }

    @Override
    public int getTransactionTimeout(Method method) {
        return -1; // un-configured
    }
}
