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

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ee.component.NamespaceConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.security.AuthorizationInterceptor;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.security.SecurityContextInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * The security-domain, if any, for this bean
     */
    private String securityDomain;

    /**
     * The @DeclareRoles (a.k.a security-role-ref) for the bean
     */
    private final Set<String> declaredRoles = new HashSet<String>();

    /**
     * The @RunAs role associated with this bean, if any
     */
    private String runAsRole;

    private Map<String, Collection<EJBMethodIdentifier>> denyAllApplicableMethods = new HashMap<String, Collection<EJBMethodIdentifier>>();

    private Map<String, Collection<String>> denyAllApplicableClasses = new HashMap<String, Collection<String>>();

    /**
     * Stores around invoke methods that are referenced in the DD that cannot be resolved until the module is loaded
     */
    private final List<String> aroundInvokeDDMethods = new ArrayList<String>(0);
    private final List<String> preDestroyDDMethods = new ArrayList<String>(0);
    private final List<String> postConstructDDMethods = new ArrayList<String>(0);


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

    private final Map<String, TransactionAttributeType> txStyle1 = new HashMap<String, TransactionAttributeType>();
    private final Map<String, TransactionAttributeType> txStyle2 = new HashMap<String, TransactionAttributeType>();
    private final PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>> txStyle3 = new PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>>() {
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
        getConfigurators().add(new EjbJarConfigurationConfigurator());

        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addComponentInterceptor(new SecurityContextInterceptorFactory(), InterceptorOrder.Component.SECURITY_CONTEXT, true);
            }
        });

        // setup a dependency on the EJBUtilities service
        this.addDependency(EJBUtilities.SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
        // setup a current invocation interceptor
        this.addCurrentInvocationContextFactory();
        // setup security interceptors
        this.setupSecurityInterceptors();

    }

    private static <K, V> V get(Map<K, V> map, K key) {
        if (map == null)
            return null;
        return map.get(key);
    }

    public TransactionAttributeType getTransactionAttribute(MethodIntf methodIntf, String className, String methodName, String... methodParams) {
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
        txAttr = get(get(get(txStyle3, className), methodName), methodParamsKey);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txStyle2, methodName);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txStyle1, className);
        if (txAttr != null)
            return txAttr;
        return beanTransactionAttribute;
    }

    public TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    /**
     * Style 1 (13.3.7.2.1 @1)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     */
    public void setTransactionAttribute(MethodIntf methodIntf, String className, TransactionAttributeType transactionAttribute) {
        if (methodIntf != null && className != null)
            throw new IllegalArgumentException("both methodIntf and className are set on " + getComponentName());
        if (methodIntf == null) {
            txStyle1.put(className, transactionAttribute);
        } else
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
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute, final String className, String methodName, String... methodParams) {
        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        if (methodIntf == null)
            txStyle3.pick(className).pick(methodName).put(methodParamsKey, transactionAttribute);
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

    protected void setupViewInterceptors(ViewDescription view) {
        this.addCurrentInvocationContextFactory(view);
    }

    protected void setupClientViewInterceptors(ViewDescription view) {
        // add a client side interceptor which handles the toString() method invocation on the bean's views
        this.addToStringMethodInterceptor(view);
    }

    /**
     * Setup the current invocation context interceptor, which will be used during the post-construct
     * lifecycle of the component instance
     */
    protected abstract void addCurrentInvocationContextFactory();

    /**
     * Setup the current invocation context interceptor, which will be used during the invocation on the view (methods)
     *
     * @param view The view for which the interceptor has to be setup
     */
    protected abstract void addCurrentInvocationContextFactory(ViewDescription view);

    protected void setupSecurityInterceptors() {
        // setup security interceptor for the component
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addComponentInterceptor(new ImmediateInterceptorFactory(new AuthorizationInterceptor()), InterceptorOrder.Component.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR, true);
            }
        });
    }

    private void addToStringMethodInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final Method TO_STRING_METHOD;
                try {
                    TO_STRING_METHOD = Object.class.getMethod("toString", new Class<?>[0]);
                } catch (NoSuchMethodException nsme) {
                    throw new DeploymentUnitProcessingException(nsme);
                }
                Method[] methods = configuration.getProxyFactory().getCachedMethods();
                for (Method method : methods) {
                    if (TO_STRING_METHOD.equals(method)) {
                        configuration.addClientInterceptor(method, new ImmediateInterceptorFactory(new ToStringMethodInterceptor(EJBComponentDescription.this.getComponentName())), InterceptorOrder.Client.TO_STRING);
                        return;
                    }
                }
            }
        });
    }

    public boolean isMessageDriven() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }

    public boolean isStateful() {
        return false;
    }

    public boolean isStateless() {
        return false;
    }

    public void addDeclaredRoles(String... roles) {
        this.declaredRoles.addAll(Arrays.asList(roles));
    }

    public void setDeclaredRoles(Collection<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Cannot set security roles to null");
        }
        this.declaredRoles.clear();
        this.declaredRoles.addAll(roles);
    }

    public Set<String> getDeclaredRoles() {
        return Collections.unmodifiableSet(this.declaredRoles);
    }

    public void setRunAs(String role) {
        this.runAsRole = role;
    }

    public String getRunAs() {
        return this.runAsRole;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void applyDenyAllOnAllViewsForClass(final String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Classname cannot be null or empty: " + className);
        }
        for (final ViewDescription view : this.getViews()) {
            Collection<String> denyAllClasses = this.denyAllApplicableClasses.get(view.getViewClassName());
            if (denyAllClasses == null) {
                denyAllClasses = new HashSet<String>();
                this.denyAllApplicableClasses.put(view.getViewClassName(), denyAllClasses);
            }
            denyAllClasses.add(className);
        }
    }

    public void applyDenyAllOnAllViewsForMethod(final EJBMethodIdentifier ejbMethodIdentifier) {
        for (final ViewDescription view : this.getViews()) {
            Collection<EJBMethodIdentifier> denyAllViewMethods = this.denyAllApplicableMethods.get(view.getViewClassName());
            if (denyAllViewMethods == null) {
                denyAllViewMethods = new ArrayList<EJBMethodIdentifier>();
                this.denyAllApplicableMethods.put(view.getViewClassName(), denyAllViewMethods);
            }
            denyAllViewMethods.add(ejbMethodIdentifier);
        }
    }

    public void applyDenyAllOnViewTypeForMethod(final MethodIntf viewType, final EJBMethodIdentifier ejbMethodIdentifier) {
        // find the right view(s) to apply the @DenyAll
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            Collection<EJBMethodIdentifier> denyAllViewMethods = this.denyAllApplicableMethods.get(view.getViewClassName());
            if (denyAllViewMethods == null) {
                denyAllViewMethods = new ArrayList<EJBMethodIdentifier>();
                this.denyAllApplicableMethods.put(view.getViewClassName(), denyAllViewMethods);
            }
            denyAllViewMethods.add(ejbMethodIdentifier);
        }
    }

    public void applyDenyAllOnAllMethodsOfAllViews() {
        // "All methods" implies a class level @DenyAll (a.k.a exclude-list)
        this.applyDenyAllOnAllViewsForClass(this.getEJBClassName());
    }

    public void applyDenyAllOnAllMethodsOfViewType(final MethodIntf viewType) {
        // find the right view(s) to apply the @DenyAll
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            // now apply the @DenyAll on class level for this view
            Collection<String> denyAllApplicableClasses = this.denyAllApplicableClasses.get(ejbView.getViewClassName());
            if (denyAllApplicableClasses == null) {
                denyAllApplicableClasses = new HashSet<String>();
                this.denyAllApplicableClasses.put(ejbView.getViewClassName(), denyAllApplicableClasses);
            }
            denyAllApplicableClasses.add(this.getEJBClassName());
        }
    }

    public Collection<EJBMethodIdentifier> getDenyAllMethodsForView(final String viewClassName) {
        final Collection<EJBMethodIdentifier> denyAllMethods = this.denyAllApplicableMethods.get(viewClassName);
        if (denyAllMethods != null) {
            return Collections.unmodifiableCollection(denyAllMethods);
        }
        return Collections.emptySet();
    }

    public boolean isDenyAllApplicableToClass(final String viewClassName, final String className) {
        final Collection<String> denyAllApplicableClasses = this.denyAllApplicableClasses.get(viewClassName);
        if (denyAllApplicableClasses == null) {
            return false;
        }
        return denyAllApplicableClasses.contains(className);
    }

    /**
     * A {@link ComponentConfigurator} which picks up {@link EjbJarConfiguration} from the attachment of the deployment
     * unit and sets it to the {@link EJBComponentCreateServiceFactory component create service factory} of the component
     * configuration.
     * <p/>
     * The component configuration is expected to be set with {@link EJBComponentCreateServiceFactory}, as its create
     * service factory, before this {@link EjbJarConfigurationConfigurator} is run.
     */
    private class EjbJarConfigurationConfigurator implements ComponentConfigurator {

        @Override
        public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
            final EjbJarConfiguration ejbJarConfiguration = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_CONFIGURATION);
            if (ejbJarConfiguration == null) {
                throw new DeploymentUnitProcessingException("EjbJarConfiguration not found as an attachment in deployment unit: " + deploymentUnit);
            }
            final EJBComponentCreateServiceFactory ejbComponentCreateServiceFactory = (EJBComponentCreateServiceFactory) configuration.getComponentCreateServiceFactory();
            ejbComponentCreateServiceFactory.setEjbJarConfiguration(ejbJarConfiguration);
        }
    }

    /**
     * {@link Interceptor} which returns the string representation for the view instance on which the {@link Object#toString()}
     * invocation happened. This interceptor (for performance reasons) does *not* check whether the invoked method is <code>toString()</code>
     * method, so it's the responsibility of the component to setup this interceptor *only* on <code>toString()</code> method on the component
     * views.
     */
    private static class ToStringMethodInterceptor implements Interceptor {

        private final String name;

        public ToStringMethodInterceptor(final String name) {
            this.name = name;
        }


        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            final ComponentViewInstance componentViewInstance = context.getPrivateData(ComponentViewInstance.class);
            if (componentViewInstance == null) {
                throw new IllegalStateException("ComponentViewInstance not available in interceptor context: " + context);
            }
            return "Proxy for view class: " + componentViewInstance.getViewClass().getName() + " of EJB: " + name;
        }
    }

    public List<String> getAroundInvokeDDMethods() {
        return aroundInvokeDDMethods;
    }

    public List<String> getPostConstructDDMethods() {
        return postConstructDDMethods;
    }

    public List<String> getPreDestroyDDMethods() {
        return preDestroyDDMethods;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "serviceName=" + getServiceName() +
                '}' + "@" + Integer.toHexString(hashCode());
    }
}
