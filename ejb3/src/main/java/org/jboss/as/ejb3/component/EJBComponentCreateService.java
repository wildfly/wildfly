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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jaikiran Pai
 */
public class EJBComponentCreateService extends BasicComponentCreateService {

    private final ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> txAttrs;

    private final TransactionManagementType transactionManagementType;

    private final EjbJarConfiguration ejbJarConfiguration;

    private final Map<String, ServiceName> viewServices;

    private final EJBSecurityMetaData securityMetaData;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public EJBComponentCreateService(final ComponentConfiguration componentConfiguration, final EjbJarConfiguration ejbJarConfiguration) {
        super(componentConfiguration);

        this.ejbJarConfiguration = ejbJarConfiguration;

        EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        this.transactionManagementType = ejbComponentDescription.getTransactionManagementType();

        // CMTTx
        if (transactionManagementType.equals(TransactionManagementType.CONTAINER)) {
            // slurp some memory
            this.txAttrs = new ConcurrentHashMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>>();
        } else {
            this.txAttrs = null;
        }
        // Setup the security metadata for the bean
        this.securityMetaData = new EJBSecurityMetaData(componentConfiguration);

        List<ViewConfiguration> views = componentConfiguration.getViews();
        if (views != null) {
            for (ViewConfiguration view : views) {
                final EJBViewConfiguration ejbView = (EJBViewConfiguration) view;
                final MethodIntf viewType = ejbView.getMethodIntf();
                for (Method method : view.getProxyFactory().getCachedMethods()) {
                    // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                    final Method componentMethod = getComponentMethod(componentConfiguration, method.getName(), method.getParameterTypes());
                    this.processTxAttr(ejbComponentDescription, viewType, componentMethod);
                }
            }
        }
        // FIXME: TODO: a temporary measure until EJBTHREE-2120 is fully resolved, let's create tx attribute map
        // for the component methods. Once the issue is resolved, we should get rid of this block and just rely on setting
        // up the tx attributes only for the views exposed by this component
        // AS7-899: We only want to process public methods of the proper sub-class. (getDefinedComponentMethods returns all in random order)
        // TODO: use ClassReflectionIndex (low prio, because we store the result without class name) (which is a bug: AS7-905)
        for (Method method : componentConfiguration.getComponentClass().getMethods()) {
            this.processTxAttr(ejbComponentDescription, MethodIntf.BEAN, method);
        }
        final HashMap<String, ServiceName> viewServices = new HashMap<String, ServiceName>();
        for (ViewDescription view : componentConfiguration.getComponentDescription().getViews()) {
            viewServices.put(view.getViewClassName(), view.getServiceName());
        }
        this.viewServices = viewServices;
    }

    private static Method getComponentMethod(final ComponentConfiguration componentConfiguration, final String name, final Class<?>[] parameterTypes) {
        try {
            return componentConfiguration.getComponentClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> getTxAttrs() {
        return txAttrs;
    }

    TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    EjbJarConfiguration getEjbJarConfiguration() {
        return this.ejbJarConfiguration;
    }

    private void processTxAttr(final EJBComponentDescription ejbComponentDescription, final MethodIntf methodIntf, final Method method) {
        if (this.getTransactionManagementType().equals(TransactionManagementType.BEAN)) {
            // it's a BMT bean
            return;
        }

        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        TransactionAttributeType txAttr = ejbComponentDescription.getTransactionAttribute(methodIntf, className, methodName, toString(method.getParameterTypes()));

        ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>> perMethodIntf = this.txAttrs.get(methodIntf);
        if (perMethodIntf == null) {
            perMethodIntf = new ConcurrentHashMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>();
            this.txAttrs.put(methodIntf, perMethodIntf);
        }
        ConcurrentMap<ArrayKey, TransactionAttributeType> perMethod = perMethodIntf.get(methodName);
        if (perMethod == null) {
            perMethod = new ConcurrentHashMap<ArrayKey, TransactionAttributeType>();
            perMethodIntf.put(methodName, perMethod);
        }
        perMethod.put(new ArrayKey((Object[]) method.getParameterTypes()), txAttr);
    }

    private static String[] toString(Class<?>[] a) {
        final String[] result = new String[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = a[i].getName();
        }
        return result;
    }

    public Map<String, ServiceName> getViewServices() {
        return viewServices;
    }

    public EJBSecurityMetaData getSecurityMetaData() {
        return this.securityMetaData;
    }
}
