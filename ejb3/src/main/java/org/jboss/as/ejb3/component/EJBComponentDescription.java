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


import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBLocalObject;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.NamespaceConfigurator;
import org.jboss.as.ee.component.NamespaceViewConfigurator;
import org.jboss.as.ee.component.TCCLInterceptor;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.naming.ContextInjectionSource;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.interceptors.EjbExceptionTransformingInterceptorFactories;
import org.jboss.as.ejb3.component.interceptors.LoggingInterceptor;
import org.jboss.as.ejb3.deployment.ApplicableMethodInformation;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsViewConfigurator;
import org.jboss.as.ejb3.security.EJBMethodSecurityAttribute;
import org.jboss.as.ejb3.security.EJBSecurityViewConfigurator;
import org.jboss.as.ejb3.security.SecurityContextInterceptorFactory;
import org.jboss.as.ejb3.timerservice.AutoTimer;
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponentDescription extends ComponentDescription {

    /**
     * EJB 3.1 FR 13.3.1, the default transaction management type is container-managed transaction demarcation.
     */
    private TransactionManagementType transactionManagementType = TransactionManagementType.CONTAINER;

    /**
     * The deployment descriptor information for this bean, if any
     */
    private EnterpriseBeanMetaData descriptorData;

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

    /**
     * The @RunAsPrincipal associated with this bean, if any
     */
    private String runAsPrincipal;

    /**
     * Roles mapped with secuirty-role
     */
    private SecurityRolesMetaData securityRoles;

    /**
     * Security role links. The key is the "from" role name and the value is a collection of "to" role names of the link.
     */
    private final Map<String, Collection<String>> securityRoleLinks = new HashMap<String, Collection<String>>();


    private final ApplicableMethodInformation<EJBMethodSecurityAttribute> methodPermissions;

    /**
     * @Schedule methods
     */
    private final Map<Method, List<AutoTimer>> scheduleMethods = new IdentityHashMap<Method, List<AutoTimer>>();

    /**
     * The actual timeout method
     */
    private Method timeoutMethod;

    /**
     * The EJB 2.x local view
     */
    private EJBViewDescription ejbLocalView;

    /**
     * The ejb local home view
     */
    private EjbHomeViewDescription ejbLocalHomeView;

    /**
     * The EJB 2.x remote view
     */
    private EJBViewDescription ejbRemoteView;

    /**
     * The ejb local home view
     */
    private EjbHomeViewDescription ejbHomeView;
    /**
     * TODO: this should not be part of the description
     */
    private TimerService timerService = NonFunctionalTimerService.INSTANCE;

    /**
     * If true this component is accessible via CORBA
     */
    private boolean exposedViaIiop = false;

    /**
     * The transaction attributes
     */
    private final ApplicableMethodInformation<TransactionAttributeType> transactionAttributes;

    /**
     * The transaction timeouts
     */
    private final ApplicableMethodInformation<Integer> transactionTimeouts;

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module
     */
    public EJBComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription.getEEModuleDescription(), deploymentUnitServiceName);
        if (ejbJarDescription.isWar()) {
            setNamingMode(ComponentNamingMode.USE_MODULE);
        } else {
            setNamingMode(ComponentNamingMode.CREATE);
        }

        getConfigurators().addFirst(new NamespaceConfigurator());
        getConfigurators().add(new EjbJarConfigurationConfigurator());

        // setup a dependency on the EJBUtilities service
        this.addDependency(EJBUtilities.SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
        // setup a current invocation interceptor
        this.addCurrentInvocationContextFactory();
        // setup a dependency on EJB remote tx repository service, if this EJB exposes atleast one remote view
        this.addRemoteTransactionsRepositoryDependency();
        this.transactionAttributes = new ApplicableMethodInformation<TransactionAttributeType>(componentName, TransactionAttributeType.REQUIRED);
        this.transactionTimeouts = new ApplicableMethodInformation<Integer>(componentName, null);
        this.methodPermissions = new ApplicableMethodInformation<EJBMethodSecurityAttribute>(componentName, null);



        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                //make sure java:comp/env is always available, even if nothing is bound there
                if(description.getNamingMode() == ComponentNamingMode.CREATE) {
                    description.getBindingConfigurations().add(new BindingConfiguration("java:comp/env", new ContextInjectionSource("env")));
                }

                if (description.isTimerServiceApplicable()) {

                    configuration.addTimeoutViewInterceptor(new ImmediateInterceptorFactory(new TCCLInterceptor(configuration.getModuleClassLoder())), InterceptorOrder.View.TCCL_INTERCEPTOR);
                    configuration.addTimeoutViewInterceptor(configuration.getNamespaceContextInterceptorFactory(), InterceptorOrder.View.JNDI_NAMESPACE_INTERCEPTOR);
                    configuration.addTimeoutViewInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
                    if (isSecurityEnabled()) {
                        configuration.addTimeoutViewInterceptor(new SecurityContextInterceptorFactory(), InterceptorOrder.View.SECURITY_CONTEXT);
                    }
                    for (final Method method : configuration.getClassIndex().getClassMethods()) {
                        configuration.addTimeoutViewInterceptor(method, new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(method)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                    }
                }
            }
        });
    }

    public void addLocalHome(final String localHome) {
        final EjbHomeViewDescription view = new EjbHomeViewDescription(this, localHome, MethodIntf.LOCAL_HOME);
        view.getConfigurators().add(new Ejb2ViewTypeConfigurator(Ejb2xViewType.LOCAL_HOME));
        getViews().add(view);
        // setup server side view interceptors
        setupViewInterceptors(view);
        // setup client side view interceptors
        setupClientViewInterceptors(view);
        // return created view
        this.ejbLocalHomeView = view;
    }

    public void addRemoteHome(final String remoteHome) {
        final EjbHomeViewDescription view = new EjbHomeViewDescription(this, remoteHome, MethodIntf.HOME);
        view.getConfigurators().add(new Ejb2ViewTypeConfigurator(Ejb2xViewType.HOME));
        getViews().add(view);
        // setup server side view interceptors
        setupViewInterceptors(view);
        // setup client side view interceptors
        setupClientViewInterceptors(view);

        // return created view
        this.ejbHomeView = view;
    }

    public void addEjbLocalObjectView(final String viewClassName) {
        final EJBViewDescription view = registerView(viewClassName, MethodIntf.LOCAL, true);
        view.getConfigurators().add(new Ejb2ViewTypeConfigurator(Ejb2xViewType.LOCAL));
        this.ejbLocalView = view;
    }

    public void addEjbObjectView(final String viewClassName) {
        final EJBViewDescription view = registerView(viewClassName, MethodIntf.REMOTE, true);
        view.getConfigurators().add(new Ejb2ViewTypeConfigurator(Ejb2xViewType.REMOTE));
        this.ejbRemoteView = view;
    }

    public TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
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

    protected void setupViewInterceptors(final EJBViewDescription view) {
        // add a logging interceptor (to take care of EJB3 spec, section 14.3 logging requirements)
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
                viewConfiguration.addViewInterceptor(LoggingInterceptor.FACTORY, InterceptorOrder.View.EJB_EXCEPTION_LOGGING_INTERCEPTOR);
                viewConfiguration.addViewInterceptor(new ImmediateInterceptorFactory(new TCCLInterceptor(componentConfiguration.getModuleClassLoder())), InterceptorOrder.View.TCCL_INTERCEPTOR);

                //If this is the EJB 2.x local or home view add the exception transformer interceptor
                if (view.getMethodIntf() == MethodIntf.LOCAL && EJBLocalObject.class.isAssignableFrom(viewConfiguration.getViewClass())) {
                    viewConfiguration.addViewInterceptor(EjbExceptionTransformingInterceptorFactories.LOCAL_INSTANCE, InterceptorOrder.View.REMOTE_EXCEPTION_TRANSFORMER);
                } else if (view.getMethodIntf() == MethodIntf.LOCAL_HOME) {
                    viewConfiguration.addViewInterceptor(EjbExceptionTransformingInterceptorFactories.LOCAL_INSTANCE, InterceptorOrder.View.REMOTE_EXCEPTION_TRANSFORMER);
                }
            }
        });
        this.addCurrentInvocationContextFactory(view);
        this.setupSecurityInterceptors(view);
        this.setupRemoteViewInterceptors(view);
        view.getConfigurators().addFirst(new NamespaceViewConfigurator());


    }

    private void setupRemoteViewInterceptors(final EJBViewDescription view) {
        if (view.getMethodIntf() == MethodIntf.REMOTE || view.getMethodIntf() == MethodIntf.HOME) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    if (Remote.class.isAssignableFrom(configuration.getViewClass())) {
                        configuration.addViewInterceptor(EjbExceptionTransformingInterceptorFactories.REMOTE_INSTANCE, InterceptorOrder.View.REMOTE_EXCEPTION_TRANSFORMER);
                    }
                }
            });
            if (view.getMethodIntf() == MethodIntf.HOME) {
                view.getConfigurators().add(new ViewConfigurator() {
                    @Override
                    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                        if (Remote.class.isAssignableFrom(configuration.getViewClass())) {
                            final String earApplicationName = componentConfiguration.getComponentDescription().getModuleDescription().getEarApplicationName();
                            configuration.setViewInstanceFactory(new RemoteHomeViewInstanceFactory(earApplicationName, componentConfiguration.getModuleName(), componentConfiguration.getComponentDescription().getModuleDescription().getDistinctName(), componentConfiguration.getComponentName()));
                        }
                    }
                });
            }
            // add the remote tx propogating interceptor
            view.getConfigurators().add(new EJBRemoteTransactionsViewConfigurator());
        }

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

    /**
     * Adds a dependency for the ComponentConfiguration, on the {@link EJBRemoteTransactionsRepository} service,
     * if the EJB exposes atleast one remote view
     */
    protected void addRemoteTransactionsRepositoryDependency() {
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
                if (this.hasRemoteView((EJBComponentDescription) description)) {
                    // add a dependency on EJBRemoteTransactionsRepository service
                    componentConfiguration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                        @Override
                        public void configureDependency(ServiceBuilder<?> serviceBuilder, EJBComponentCreateService ejbComponentCreateService) throws DeploymentUnitProcessingException {
                            serviceBuilder.addDependency(EJBRemoteTransactionsRepository.SERVICE_NAME, EJBRemoteTransactionsRepository.class, ejbComponentCreateService.getEJBRemoteTransactionsRepositoryInjector());
                        }
                    });
                }
            }

            /**
             * Returns true if the passed EJB component description has atleast one remote view
             * @param ejbComponentDescription
             * @return
             */
            private boolean hasRemoteView(final EJBComponentDescription ejbComponentDescription) {
                final Set<ViewDescription> views = ejbComponentDescription.getViews();
                for (final ViewDescription view : views) {
                    if (!(view instanceof EJBViewDescription)) {
                        continue;
                    }
                    final MethodIntf viewType = ((EJBViewDescription) view).getMethodIntf();
                    if (viewType == MethodIntf.REMOTE || viewType == MethodIntf.HOME) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    protected void setupSecurityInterceptors(final ViewDescription view) {
        // setup security interceptor for the component
        view.getConfigurators().add(new EJBSecurityViewConfigurator());
    }

    private void addToStringMethodInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final Method TO_STRING_METHOD;
                try {
                    TO_STRING_METHOD = Object.class.getMethod("toString");
                } catch (NoSuchMethodException nsme) {
                    throw new DeploymentUnitProcessingException(nsme);
                }
                List<Method> methods = configuration.getProxyFactory().getCachedMethods();
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
            throw MESSAGES.SecurityRolesIsNull();
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

    public void setRunAsPrincipal(String principal) {
        this.runAsPrincipal = principal;
    }

    public String getRunAsPrincipal() {
        return runAsPrincipal;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public SecurityRolesMetaData getSecurityRoles() {
        return securityRoles;
    }

    public void setSecurityRoles(SecurityRolesMetaData securityRoles) {
        this.securityRoles = securityRoles;
    }

    public void linkSecurityRoles(final String fromRole, final String toRole) {
        if (fromRole == null || fromRole.trim().isEmpty()) {
            throw MESSAGES.failToLinkFromEmptySecurityRole(fromRole);
        }
        if (toRole == null || toRole.trim().isEmpty()) {
            throw MESSAGES.failToLinkToEmptySecurityRole(toRole);
        }

        Collection<String> roleLinks = this.securityRoleLinks.get(fromRole);
        if (roleLinks == null) {
            roleLinks = new HashSet<String>();
            this.securityRoleLinks.put(fromRole, roleLinks);
        }
        roleLinks.add(toRole);
    }

    protected EJBViewDescription registerView(final String viewClassName, final MethodIntf viewType) {
        return registerView(viewClassName, viewType, false);
    }

    protected EJBViewDescription registerView(final String viewClassName, final MethodIntf viewType, final boolean ejb2xView) {
        // setup the ViewDescription
        final EJBViewDescription viewDescription = new EJBViewDescription(this, viewClassName, viewType, ejb2xView);
        getViews().add(viewDescription);
        // setup server side view interceptors
        setupViewInterceptors(viewDescription);
        // setup client side view interceptors
        setupClientViewInterceptors(viewDescription);
        // return created view
        return viewDescription;
    }

    public Map<String, Collection<String>> getSecurityRoleLinks() {
        return Collections.unmodifiableMap(this.securityRoleLinks);
    }


    /**
     * Returns true if this bean is secured. Else returns false.
     *
     * @return
     */
    public boolean isSecurityEnabled() {
        return this.securityDomain != null;
    }

    private static class Ejb2ViewTypeConfigurator implements ViewConfigurator {
        private final Ejb2xViewType local;

        public Ejb2ViewTypeConfigurator(final Ejb2xViewType local) {
            this.local = local;
        }

        @Override
        public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {

            configuration.putPrivateData(Ejb2xViewType.class, local);
        }
    }

    /**
     * A {@link ComponentConfigurator} which picks up {@link org.jboss.as.ejb3.deployment.ApplicationExceptions} from the attachment of the deployment
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
            final ApplicationExceptions appExceptions = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.APPLICATION_EXCEPTION_DETAILS);
            if (appExceptions == null) {
                throw MESSAGES.ejbJarConfigNotFound(deploymentUnit);
            }
            final EJBComponentCreateServiceFactory ejbComponentCreateServiceFactory = (EJBComponentCreateServiceFactory) configuration.getComponentCreateServiceFactory();
            ejbComponentCreateServiceFactory.setEjbJarConfiguration(appExceptions);
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
            final ComponentView componentView = context.getPrivateData(ComponentView.class);
            if (componentView == null) {
                throw MESSAGES.componentViewNotAvailableInContext(context);
            }
            return "Proxy for view class: " + componentView.getViewClass().getName() + " of EJB: " + name;
        }
    }


    public TimerService getTimerService() {
        return timerService;
    }

    public void setTimerService(final TimerService timerService) {
        this.timerService = timerService;
    }

    public EnterpriseBeanMetaData getDescriptorData() {
        return descriptorData;
    }

    public void setDescriptorData(final EnterpriseBeanMetaData descriptorData) {
        this.descriptorData = descriptorData;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
    }

    public void setTimeoutMethod(final Method timeoutMethod) {
        this.timeoutMethod = timeoutMethod;
    }

    public Map<Method, List<AutoTimer>> getScheduleMethods() {
        return Collections.unmodifiableMap(scheduleMethods);
    }

    public void addScheduleMethod(final Method method, final AutoTimer timer) {
        List<AutoTimer> schedules = scheduleMethods.get(method);
        if (schedules == null) {
            scheduleMethods.put(method, schedules = new ArrayList<AutoTimer>(1));
        }
        schedules.add(timer);
    }

    public EJBViewDescription getEjbLocalView() {
        return ejbLocalView;
    }

    public EjbHomeViewDescription getEjbLocalHomeView() {
        return ejbLocalHomeView;
    }

    public EjbHomeViewDescription getEjbHomeView() {
        return ejbHomeView;
    }

    public EJBViewDescription getEjbRemoteView() {
        return ejbRemoteView;
    }

    public boolean isExposedViaIiop() {
        return exposedViaIiop;
    }

    public void setExposedViaIiop(final boolean exposedViaIiop) {
        this.exposedViaIiop = exposedViaIiop;
    }

    public ApplicableMethodInformation<TransactionAttributeType> getTransactionAttributes() {
        return transactionAttributes;
    }

    public ApplicableMethodInformation<Integer> getTransactionTimeouts() {
        return transactionTimeouts;
    }

    public ApplicableMethodInformation<EJBMethodSecurityAttribute> getMethodPermissions() {
        return methodPermissions;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "serviceName=" + getServiceName() +
                '}' + "@" + Integer.toHexString(hashCode());
    }
}
