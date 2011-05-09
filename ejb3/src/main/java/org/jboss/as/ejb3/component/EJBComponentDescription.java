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

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.NamespaceConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponentDescription extends ComponentDescription {
    /**
     * EJB 3.1 FR 13.3.7, the default transaction attribute is <i>REQUIRED</i>.
     */
    private TransactionAttributeType beanTransactionAttribute = TransactionAttributeType.REQUIRED;
    /**
     * EJB 3.1 FR 13.3.1, the default transaction management type is container-managed transaction demarcation.
     */
    private TransactionManagementType transactionManagementType = TransactionManagementType.CONTAINER;

    private final Map<MethodIntf, TransactionAttributeType> txPerViewStyle1 = new HashMap<MethodIntf, TransactionAttributeType>();
    private final PopulatingMap<MethodIntf, Map<String, TransactionAttributeType>> txPerViewStyle2 = new PopulatingMap<MethodIntf, Map<String, TransactionAttributeType>>() {
        @Override
        Map<String, TransactionAttributeType> populate() {
            return new HashMap<String, TransactionAttributeType>();
        }
    };
    private final PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>> txPerViewStyle3 = new PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>>() {
        @Override
        PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>> populate() {
            return new PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>() {
                @Override
                Map<ArrayKey, TransactionAttributeType> populate() {
                    return new HashMap<ArrayKey, TransactionAttributeType>();
                }
            };
        }
    };

    // style 1 == beanTransactionAttribute
    private final Map<String, TransactionAttributeType> txStyle2 = new HashMap<String, TransactionAttributeType>();
    private final PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>> txStyle3 = new PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>() {
        @Override
        Map<ArrayKey, TransactionAttributeType> populate() {
            return new HashMap<ArrayKey, TransactionAttributeType>();
        }
    };

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module
     */
    public EJBComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription.getEEModuleDescription(), ejbJarDescription.getEEModuleDescription().getOrAddClassByName(componentClassName), deploymentUnitServiceName);
        if (ejbJarDescription.isWar()) {
            setNamingMode(ComponentNamingMode.USE_MODULE);
        } else {
            setNamingMode(ComponentNamingMode.CREATE);
        }

        getConfigurators().addFirst(new NamespaceConfigurator());

        // setup a dependency on the EJBUtilities service
        this.addDependency(EJBUtilities.SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
    }

    private static <K, V> V get(Map<K, V> map, K key) {
        if (map == null)
            return null;
        return map.get(key);
    }

    public TransactionAttributeType getTransactionAttribute(MethodIntf methodIntf, String methodName, String... methodParams) {
        assert methodIntf != null : "methodIntf is null";
        assert methodName != null : "methodName is null";
        assert methodParams != null : "methodParams is null";

        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        TransactionAttributeType txAttr = get(get(get(txPerViewStyle3, methodIntf), methodName), methodParamsKey);
        if (txAttr != null)
            return txAttr;
        txAttr = get(get(txPerViewStyle2, methodIntf), methodName);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txPerViewStyle1, methodIntf);
        if (txAttr != null)
            return txAttr;
        txAttr = get(get(txStyle3, methodName), methodParamsKey);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txStyle2, methodName);
        if (txAttr != null)
            return txAttr;
        return beanTransactionAttribute;
    }

    public TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    public abstract MethodIntf getMethodIntf(String viewClassName);

    /**
     * Style 1 (13.3.7.2.1 @1)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     */
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute) {
        if (methodIntf == null)
            this.beanTransactionAttribute = transactionAttribute;
        else
            txPerViewStyle1.put(methodIntf, transactionAttribute);
    }

    /**
     * Style 2 (13.3.7.2.1 @2)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     */
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute, String methodName) {
        if (methodIntf == null)
            txStyle2.put(methodName, transactionAttribute);
        else
            txPerViewStyle2.pick(methodIntf).put(methodName, transactionAttribute);
    }

    /**
     * Style 3 (13.3.7.2.1 @3)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     * @param methodParams
     */
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute, String methodName, String... methodParams) {
        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        if (methodIntf != null)
            txStyle3.pick(methodName).put(methodParamsKey, transactionAttribute);
        else
            txPerViewStyle3.pick(methodIntf).pick(methodName).put(methodParamsKey, transactionAttribute);
    }

    public void setTransactionManagementType(TransactionManagementType transactionManagementType) {
        this.transactionManagementType = transactionManagementType;
    }

    public String getEJBName() {
        return this.getComponentName();
    }

    public String getEJBClassName() {
        return this.getComponentClassName();
    }


//    protected void processComponentMethod(ComponentConfiguration configuration, Method componentMethod) throws DeploymentUnitProcessingException {
//        super.processComponentMethod(configuration, componentMethod);
//
//        // TODO: a temporary measure until EJBTHREE-2120 is fully resolved
//        MethodIntf methodIntf = MethodIntf.BEAN;
//        processTxAttr((EJBComponentConfiguration) configuration, methodIntf, componentMethod);
//    }
//
//    @Override
//    protected void processViewMethod(ComponentConfiguration configuration, Class<?> viewClass, Method viewMethod, Method componentMethod) {
//        super.processViewMethod(configuration, viewClass, viewMethod, componentMethod);
//
//        MethodIntf methodIntf = getMethodIntf(viewClass.getName());
//        processTxAttr((EJBComponentConfiguration) configuration, methodIntf, viewMethod);
//    }

    private void processTxAttr(EJBComponentConfiguration configuration, MethodIntf methodIntf, Method method) {
        if (configuration.getTransactionManagementType().equals(TransactionManagementType.BEAN)) {
            // it's a BMT bean
            return;
        }

        String methodName = method.getName();
        TransactionAttributeType txAttr = getTransactionAttribute(methodIntf, methodName, toString(method.getParameterTypes()));

        ConcurrentMap<MethodIntf, ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>> txAttrs = configuration.getTxAttrs();
        ConcurrentMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>> perMethodIntf = txAttrs.get(methodIntf);
        if (perMethodIntf == null) {
            perMethodIntf = new ConcurrentHashMap<String, ConcurrentMap<ArrayKey, TransactionAttributeType>>();
            txAttrs.put(methodIntf, perMethodIntf);
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

    protected void setupViewInterceptors(ViewDescription view) {
        this.addCurrentInvocationContextFactory(view);
    }

    protected void setupClientViewInterceptors(ViewDescription view) {

    }

    protected abstract void addCurrentInvocationContextFactory(ViewDescription view);
}
