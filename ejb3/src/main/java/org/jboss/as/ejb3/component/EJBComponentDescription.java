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

import static org.jboss.as.ejb3.subsystem.IdentityResourceDefinition.IDENTITY_CAPABILITY;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import javax.ejb.EJBLocalObject;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.NamespaceConfigurator;
import org.jboss.as.ee.component.NamespaceViewConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.ViewService;
import org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.naming.ContextInjectionSource;
import org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor;
import org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor;
import org.jboss.as.ejb3.component.interceptors.EjbExceptionTransformingInterceptorFactories;
import org.jboss.as.ejb3.component.interceptors.LoggingInterceptor;
import org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory;
import org.jboss.as.ejb3.component.invocationmetrics.ExecutionTimeInterceptor;
import org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor;
import org.jboss.as.ejb3.deployment.ApplicableMethodInformation;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.security.ApplicationSecurityDomainConfig;
import org.jboss.as.ejb3.security.EJBMethodSecurityAttribute;
import org.jboss.as.ejb3.security.EJBSecurityViewConfigurator;
import org.jboss.as.ejb3.security.IdentityOutflowInterceptorFactory;
import org.jboss.as.ejb3.security.PolicyContextIdInterceptor;
import org.jboss.as.ejb3.security.RoleAddingInterceptor;
import org.jboss.as.ejb3.security.RunAsPrincipalInterceptor;
import org.jboss.as.ejb3.security.SecurityContextInterceptorFactory;
import org.jboss.as.ejb3.security.SecurityDomainInterceptorFactory;
import org.jboss.as.ejb3.security.SecurityRolesAddingInterceptor;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainDefinition;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition;
import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
import org.jboss.as.ejb3.timerservice.AutoTimer;
import org.jboss.as.ejb3.timerservice.NonFunctionalTimerService;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.invocation.AccessCheckingInterceptor;
import org.jboss.invocation.ContextClassLoaderInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.security.SecurityConstants;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.authz.Roles;

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
     * The default security domain to use if no explicit security domain is configured for this bean
     */
    private String defaultSecurityDomain;

    /**
     * A function that returns the configuration for a Elytron security domain
     */
    private Function<String, ApplicationSecurityDomainConfig> knownSecurityDomain = null;

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
     * Roles mapped with security-role
     */
    private SecurityRolesMetaData securityRoles;

    /**
     * Security role links. The key is the "from" role name and the value is a collection of "to" role names of the link.
     */
    private final Map<String, Collection<String>> securityRoleLinks = new HashMap<String, Collection<String>>();


    private final ApplicableMethodInformation<EJBMethodSecurityAttribute> descriptorMethodPermissions;
    private final ApplicableMethodInformation<EJBMethodSecurityAttribute> annotationMethodPermissions;


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
    private TimerService timerService = NonFunctionalTimerService.DISABLED;

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
     * The default container interceptors
     */
    private List<InterceptorDescription> defaultContainerInterceptors = new ArrayList<InterceptorDescription>();

    /**
     * Whether or not to exclude the default container interceptors for the EJB
     */
    private boolean excludeDefaultContainerInterceptors;

    /**
     * Container interceptors applicable for all methods of the EJB
     */
    private List<InterceptorDescription> classLevelContainerInterceptors = new ArrayList<InterceptorDescription>();

    /**
     * Container interceptors applicable per method of the EJB
     */
    private Map<MethodIdentifier, List<InterceptorDescription>> methodLevelContainerInterceptors = new HashMap<MethodIdentifier, List<InterceptorDescription>>();

    /**
     * Whether or not to exclude the default container interceptors applicable for the method of the EJB
     */
    private Map<MethodIdentifier, Boolean> excludeDefaultContainerInterceptorsForMethod = new HashMap<MethodIdentifier, Boolean>();

    /**
     * Whether or not to exclude the class level container interceptors applicable for the method of the EJB
     */
    private Map<MethodIdentifier, Boolean> excludeClassLevelContainerInterceptorsForMethod = new HashMap<MethodIdentifier, Boolean>();

    /**
     * Combination of class and method level container interceptors
     */
    private Set<InterceptorDescription> allContainerInterceptors;

    /**
     * missing-method-permissions-deny-access that's used for secured EJBs
     */
    private Boolean missingMethodPermissionsDenyAccess = null;

    private String policyContextID;

    private final ShutDownInterceptorFactory shutDownInterceptorFactory = new ShutDownInterceptorFactory();

    private BooleanSupplier outflowSecurityDomainsConfigured;

    private boolean securityRequired;

    /**
     * Construct a new instance.
     *
     * @param componentName             the component name
     * @param componentClassName        the component instance class name
     * @param ejbJarDescription         the module
     * @param deploymentUnitServiceName
     * @param descriptorData            the optional descriptor metadata
     */
    public EJBComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName, final EnterpriseBeanMetaData descriptorData) {
        super(componentName, componentClassName, ejbJarDescription.getEEModuleDescription(), deploymentUnitServiceName);
        this.descriptorData = descriptorData;
        if (ejbJarDescription.isWar()) {
            setNamingMode(ComponentNamingMode.USE_MODULE);
        } else {
            setNamingMode(ComponentNamingMode.CREATE);
        }

        getConfigurators().addFirst(new NamespaceConfigurator());
        getConfigurators().add(new EjbJarConfigurationConfigurator());
        getConfigurators().add(new SecurityDomainDependencyConfigurator(this));


        // setup a current invocation interceptor
        this.addCurrentInvocationContextFactory();
        // setup a dependency on EJB remote tx repository service, if this EJB exposes at least one remote view
        this.addRemoteTransactionsDependency();
        this.transactionAttributes = new ApplicableMethodInformation<TransactionAttributeType>(componentName, TransactionAttributeType.REQUIRED);
        this.transactionTimeouts = new ApplicableMethodInformation<Integer>(componentName, null);
        this.descriptorMethodPermissions = new ApplicableMethodInformation<EJBMethodSecurityAttribute>(componentName, null);
        this.annotationMethodPermissions = new ApplicableMethodInformation<EJBMethodSecurityAttribute>(componentName, null);


        //add a dependency on the module deployment service
        //we need to make sure this is up before the EJB starts, so that remote invocations are available
        addDependency(deploymentUnitServiceName.append(ModuleDeployment.SERVICE_NAME));

        getConfigurators().addFirst(EJBValidationConfigurator.INSTANCE);
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
                String contextID = deploymentUnit.getName();
                if (deploymentUnit.getParent() != null) {
                    contextID = deploymentUnit.getParent().getName() + "!" + contextID;
                }
                policyContextID = contextID;
                //make sure java:comp/env is always available, even if nothing is bound there
                if (description.getNamingMode() == ComponentNamingMode.CREATE) {
                    description.getBindingConfigurations().add(new BindingConfiguration("java:comp/env", new ContextInjectionSource("env", "java:comp/env")));
                }
                final List<SetupAction> ejbSetupActions = deploymentUnit.getAttachmentList(Attachments.OTHER_EE_SETUP_ACTIONS);

                if (description.isTimerServiceRequired()) {

                    if (!ejbSetupActions.isEmpty()) {
                        configuration.addTimeoutViewInterceptor(AdditionalSetupInterceptor.factory(ejbSetupActions), InterceptorOrder.View.EE_SETUP);
                    }
                    configuration.addTimeoutViewInterceptor(shutDownInterceptorFactory, InterceptorOrder.View.SHUTDOWN_INTERCEPTOR);

                    final ClassLoader classLoader = configuration.getModuleClassLoader();
                    configuration.addTimeoutViewInterceptor(AccessCheckingInterceptor.getFactory(), InterceptorOrder.View.CHECKING_INTERCEPTOR);
                    configuration.addTimeoutViewInterceptor(new ImmediateInterceptorFactory(new ContextClassLoaderInterceptor(classLoader)), InterceptorOrder.View.TCCL_INTERCEPTOR);
                    configuration.addTimeoutViewInterceptor(configuration.getNamespaceContextInterceptorFactory(), InterceptorOrder.View.JNDI_NAMESPACE_INTERCEPTOR);
                    configuration.addTimeoutViewInterceptor(CurrentInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
                    EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) description;
                    final boolean securityRequired = hasBeanLevelSecurityMetadata();
                    ejbComponentDescription.setSecurityRequired(securityRequired);
                    if (ejbComponentDescription.isSecurityDomainKnown()) {
                        final HashMap<Integer, InterceptorFactory> elytronInterceptorFactories = getElytronInterceptorFactories(policyContextID, ejbComponentDescription.isEnableJacc(), true);
                        elytronInterceptorFactories.forEach((priority, elytronInterceptorFactory) -> configuration.addTimeoutViewInterceptor(elytronInterceptorFactory, priority));
                    } else if (deploymentUnit.hasAttachment(SecurityAttachments.SECURITY_ENABLED)) {
                        configuration.addTimeoutViewInterceptor(new SecurityContextInterceptorFactory(securityRequired, policyContextID), InterceptorOrder.View.SECURITY_CONTEXT);
                    }
                    final Set<Method> classMethods = configuration.getClassIndex().getClassMethods();
                    for (final Method method : classMethods) {
                        configuration.addTimeoutViewInterceptor(method, new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(method)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                    }
                }
                if (!ejbSetupActions.isEmpty()) {
                    configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                        @Override
                        public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                            for (final SetupAction setupAction : ejbSetupActions) {
                                serviceBuilder.addDependencies(setupAction.dependencies());
                            }
                        }
                    });
                }

                configuration.addComponentInterceptor(ExecutionTimeInterceptor.FACTORY, InterceptorOrder.Component.EJB_EXECUTION_TIME_INTERCEPTOR, true);
                configuration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                    @Override
                    public void configureDependency(ServiceBuilder<?> serviceBuilder, EJBComponentCreateService service) throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(LoggingInterceptor.LOGGING_ENABLED_SERVICE_NAME, AtomicBoolean.class, service.getExceptionLoggingEnabledInjector());
                    }
                });
            }
        });

        // setup dependencies on the transaction manager services
        addTransactionManagerDependencies();

        // setup ejb suspend handler dependency
        addEJBSuspendHandlerDependency();

        // setup dependency on ServerSecurityManager
        addServerSecurityManagerDependency();
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
                final ClassLoader classLoader = componentConfiguration.getModuleClassLoader();
                viewConfiguration.addViewInterceptor(AccessCheckingInterceptor.getFactory(), InterceptorOrder.View.CHECKING_INTERCEPTOR);
                viewConfiguration.addViewInterceptor(new ImmediateInterceptorFactory(new ContextClassLoaderInterceptor(classLoader)), InterceptorOrder.View.TCCL_INTERCEPTOR);

                //If this is the EJB 2.x local or home view add the exception transformer interceptor
                if (view.getMethodIntf() == MethodIntf.LOCAL && EJBLocalObject.class.isAssignableFrom(viewConfiguration.getViewClass())) {
                    viewConfiguration.addViewInterceptor(EjbExceptionTransformingInterceptorFactories.LOCAL_INSTANCE, InterceptorOrder.View.REMOTE_EXCEPTION_TRANSFORMER);
                } else if (view.getMethodIntf() == MethodIntf.LOCAL_HOME) {
                    viewConfiguration.addViewInterceptor(EjbExceptionTransformingInterceptorFactories.LOCAL_INSTANCE, InterceptorOrder.View.REMOTE_EXCEPTION_TRANSFORMER);
                }

                final List<SetupAction> ejbSetupActions = context.getDeploymentUnit().getAttachmentList(Attachments.OTHER_EE_SETUP_ACTIONS);
                if (!ejbSetupActions.isEmpty()) {
                    viewConfiguration.addViewInterceptor(AdditionalSetupInterceptor.factory(ejbSetupActions), InterceptorOrder.View.EE_SETUP);
                }
                viewConfiguration.addViewInterceptor(WaitTimeInterceptor.FACTORY, InterceptorOrder.View.EJB_WAIT_TIME_INTERCEPTOR);
                viewConfiguration.addViewInterceptor(shutDownInterceptorFactory, InterceptorOrder.View.SHUTDOWN_INTERCEPTOR);


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
     * Adds a dependency for the ComponentConfiguration on the remote transaction service if the EJB exposes at least one remote view
     */
    protected void addRemoteTransactionsDependency() {
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
                if (this.hasRemoteView((EJBComponentDescription) description)) {
                    // add a dependency on local transaction service
                    componentConfiguration.getCreateDependencies().add((sb, cs) -> sb.addDependency(TxnServices.JBOSS_TXN_REMOTE_TRANSACTION_SERVICE));
                }
            }

            /**
             * Returns true if the passed EJB component description has at least one remote view
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

    /**
     * Sets up a {@link ComponentConfigurator} which then sets up the relevant dependencies on the transaction manager services for the {@link EJBComponentCreateService}
     */
    protected void addTransactionManagerDependencies() {
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
                componentConfiguration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final EJBComponentCreateService ejbComponentCreateService) throws DeploymentUnitProcessingException {
                        // add dependency on transaction manager
                        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
                        // add dependency on UserTransaction
                        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, ejbComponentCreateService.getUserTransactionInjector());
                        // add dependency on TransactionSynchronizationRegistry
                        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY);
                    }
                });

            }
        });
    }

    /**
     * Sets up a {@link ComponentConfigurator} which then sets up the dependency on the EJBSuspendHandlerService service for the {@link EJBComponentCreateService}
     */
    protected void addEJBSuspendHandlerDependency() {
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
                componentConfiguration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                    @Override public void configureDependency(final ServiceBuilder<?> serviceBuilder, final EJBComponentCreateService ejbComponentCreateService)
                            throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(EJBSuspendHandlerService.SERVICE_NAME, EJBSuspendHandlerService.class,
                                ejbComponentCreateService.getEJBSuspendHandlerInjector());
                    }
                });
            }
        });
    }

    /**
     * Sets up a {@link ComponentConfigurator} which then sets up the dependency on the ServerSecurityManager service for the {@link EJBComponentCreateService}
     */
    protected void addServerSecurityManagerDependency() {
        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
                if (! ((EJBComponentDescription) description).isSecurityDomainKnown()) {
                    final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
                    final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
                    componentConfiguration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                        @Override
                        public void configureDependency(final ServiceBuilder<?> serviceBuilder, final EJBComponentCreateService ejbComponentCreateService) throws DeploymentUnitProcessingException {
                            serviceBuilder.addDependency(support.getCapabilityServiceName("org.wildfly.legacy-security.server-security-manager"), ServerSecurityManager.class, ejbComponentCreateService.getServerSecurityManagerInjector());
                        }
                    });
                }
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

    public boolean isEntity() {
        return false;
    }

    public boolean isMessageDriven() {
        return false;
    }

    public boolean isSession() {
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
            throw EjbLogger.ROOT_LOGGER.SecurityRolesIsNull();
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

    public void setDefaultSecurityDomain(final String defaultSecurityDomain) {
        this.defaultSecurityDomain = defaultSecurityDomain;
    }

    public void setKnownSecurityDomainFunction(final Function<String, ApplicationSecurityDomainConfig> knownSecurityDomain) {
        this.knownSecurityDomain = knownSecurityDomain;
    }

    public boolean isSecurityDomainKnown() {
        return knownSecurityDomain == null ? false : knownSecurityDomain.apply(getSecurityDomain()) != null;
    }

    public boolean isEnableJacc() {
        ApplicationSecurityDomainConfig config = knownSecurityDomain == null ? null : knownSecurityDomain.apply(getSecurityDomain());

        if (config != null) {
            return config.isEnableJacc();
        }

        return false;
    }

    public void setOutflowSecurityDomainsConfigured(final BooleanSupplier outflowSecurityDomainsConfigured) {
        this.outflowSecurityDomainsConfigured = outflowSecurityDomainsConfigured;
    }

    public boolean isOutflowSecurityDomainsConfigured() {
        return outflowSecurityDomainsConfigured.getAsBoolean();
    }

    /**
     * Returns the security domain that is applicable for this bean. In the absence of any explicit
     * configuration of a security domain for this bean, this method returns the default security domain
     * (if any) that's configured for all beans in the EJB3 subsystem
     *
     * @return
     */
    public String getSecurityDomain() {
        if (this.securityDomain == null) {
            return this.defaultSecurityDomain;
        }
        return this.securityDomain;
    }

    /**
     * Returns true if this bean has been explicitly configured with a security domain via the
     * {@link org.jboss.ejb3.annotation.SecurityDomain} annotation or via the jboss-ejb3.xml deployment descriptor.
     * Else returns false.
     *
     * @return
     */
    public boolean isExplicitSecurityDomainConfigured() {
        return this.securityDomain != null;
    }

    public void setMissingMethodPermissionsDenyAccess(Boolean missingMethodPermissionsDenyAccess) {
        this.missingMethodPermissionsDenyAccess = missingMethodPermissionsDenyAccess;
    }

    public Boolean isMissingMethodPermissionsDeniedAccess() {
        return this.missingMethodPermissionsDenyAccess;
    }

    public SecurityRolesMetaData getSecurityRoles() {
        return securityRoles;
    }

    public void setSecurityRoles(SecurityRolesMetaData securityRoles) {
        this.securityRoles = securityRoles;
    }

    public void linkSecurityRoles(final String fromRole, final String toRole) {
        if (fromRole == null || fromRole.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.failToLinkFromEmptySecurityRole(fromRole);
        }
        if (toRole == null || toRole.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.failToLinkToEmptySecurityRole(toRole);
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

        if (viewType == MethodIntf.REMOTE ||
                viewType == MethodIntf.HOME) {
            setupRemoteView(viewDescription);
        }

        return viewDescription;
    }

    protected void setupRemoteView(final EJBViewDescription viewDescription) {
        viewDescription.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.getDependencies().add(new DependencyConfigurator<ViewService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ViewService service) throws DeploymentUnitProcessingException {
                        CapabilityServiceSupport support = context.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
                        serviceBuilder.addDependency(support.getCapabilityServiceName(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY_NAME));
                    }
                });
            }
        });
    }

    public Map<String, Collection<String>> getSecurityRoleLinks() {
        return Collections.unmodifiableMap(this.securityRoleLinks);
    }


    /**
     * Returns true if this component description has any security metadata configured at the EJB level.
     * Else returns false. Note that this method does *not* consider method level security metadata.
     *
     * @return
     */
    public boolean hasBeanLevelSecurityMetadata() {
     // if an explicit security-domain is present, then we consider it the bean to be processed by security interceptors
        if (securityDomain != null) {
            return true;
        }
        // if a run-as is present, then we consider it the bean to be processed by security interceptors
        if (runAsRole != null) {
            return true;
        }
        // if a run-as-principal is present, then we consider it the bean to be processed by security interceptors
        if (runAsPrincipal != null) {
            return true;
        }
        // if security roles are configured then we consider the bean to be processed by security interceptors
        if (securityRoles != null && !securityRoles.isEmpty()) {
            return true;
        }
        // if security role links are configured then we consider the bean to be processed by security interceptors
        if (securityRoleLinks != null && !securityRoleLinks.isEmpty()) {
            return true;
        }
        // if declared roles are configured then we consider the bean to be processed by security interceptors
        if (declaredRoles != null && !declaredRoles.isEmpty()) {
            return true;
        }
        // no security metadata at bean level
        return false;
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
                throw EjbLogger.ROOT_LOGGER.ejbJarConfigNotFound(deploymentUnit);
            }
            final EJBComponentCreateServiceFactory ejbComponentCreateServiceFactory = (EJBComponentCreateServiceFactory) configuration.getComponentCreateServiceFactory();
            ejbComponentCreateServiceFactory.setEjbJarConfiguration(appExceptions);
        }
    }

    /**
     * Responsible for adding a dependency on the security domain service for the EJB component, if a security domain
     * is applicable for the bean
     */
    private class SecurityDomainDependencyConfigurator implements ComponentConfigurator {

        private final EJBComponentDescription ejbComponentDescription;

        SecurityDomainDependencyConfigurator(final EJBComponentDescription ejbComponentDescription) {
            this.ejbComponentDescription = ejbComponentDescription;
        }

        @Override
        public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            configuration.getCreateDependencies().add(new DependencyConfigurator<Service<Component>>() {
                @Override
                public void configureDependency(ServiceBuilder<?> serviceBuilder, Service<Component> service) throws DeploymentUnitProcessingException {
                    final EJBComponentCreateService ejbComponentCreateService = (EJBComponentCreateService) service;
                    final String securityDomainName = SecurityDomainDependencyConfigurator.this.ejbComponentDescription.getSecurityDomain();
                    if (SecurityDomainDependencyConfigurator.this.ejbComponentDescription.isSecurityDomainKnown()) {
                        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
                        final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
                        if (securityDomainName != null && ! securityDomainName.isEmpty()) {
                            serviceBuilder.addDependency(support.getCapabilityServiceName(ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY, securityDomainName),
                                    ApplicationSecurityDomain.class, ejbComponentCreateService.getApplicationSecurityDomainInjector());
                        }
                        if (SecurityDomainDependencyConfigurator.this.ejbComponentDescription.isOutflowSecurityDomainsConfigured()) {
                            serviceBuilder.addDependency(support.getCapabilityServiceName(IDENTITY_CAPABILITY), Function.class, ejbComponentCreateService.getIdentityOutflowFunctionInjector());
                        }
                    } else {
                        if (securityDomainName != null && !securityDomainName.isEmpty()) {
                            final ServiceName securityDomainServiceName = SecurityDomainService.SERVICE_NAME.append(securityDomainName);
                            serviceBuilder.addDependency(securityDomainServiceName);
                        }
                        serviceBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(SecurityConstants.DEFAULT_EJB_APPLICATION_POLICY));
                    }
                }
            });
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
                throw EjbLogger.ROOT_LOGGER.componentViewNotAvailableInContext(context);
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

    public ApplicableMethodInformation<EJBMethodSecurityAttribute> getDescriptorMethodPermissions() {
        return descriptorMethodPermissions;
    }

    public ApplicableMethodInformation<EJBMethodSecurityAttribute> getAnnotationMethodPermissions() {
        return annotationMethodPermissions;
    }

    public void setDefaultContainerInterceptors(final List<InterceptorDescription> defaultInterceptors) {
        this.defaultContainerInterceptors = defaultInterceptors;
    }

    public List<InterceptorDescription> getDefaultContainerInterceptors() {
        return this.defaultContainerInterceptors;
    }

    public void setClassLevelContainerInterceptors(final List<InterceptorDescription> containerInterceptors) {
        this.classLevelContainerInterceptors = containerInterceptors;
    }

    public List<InterceptorDescription> getClassLevelContainerInterceptors() {
        return this.classLevelContainerInterceptors;
    }

    public void setExcludeDefaultContainerInterceptors(boolean excludeDefaultContainerInterceptors) {
        this.excludeDefaultContainerInterceptors = excludeDefaultContainerInterceptors;
    }

    public boolean isExcludeDefaultContainerInterceptors() {
        return this.excludeDefaultContainerInterceptors;
    }

    public void excludeDefaultContainerInterceptors(final MethodIdentifier methodIdentifier) {
        this.excludeDefaultContainerInterceptorsForMethod.put(methodIdentifier, true);
    }

    public boolean isExcludeDefaultContainerInterceptors(final MethodIdentifier methodIdentifier) {
        return this.excludeDefaultContainerInterceptorsForMethod.get(methodIdentifier) != null;
    }

    public void excludeClassLevelContainerInterceptors(final MethodIdentifier methodIdentifier) {
        this.excludeClassLevelContainerInterceptorsForMethod.put(methodIdentifier, true);
    }

    public boolean isExcludeClassLevelContainerInterceptors(final MethodIdentifier methodIdentifier) {
        return this.excludeClassLevelContainerInterceptorsForMethod.get(methodIdentifier) != null;
    }

    public Map<MethodIdentifier, List<InterceptorDescription>> getMethodLevelContainerInterceptors() {
        return this.methodLevelContainerInterceptors;
    }

    public void setMethodContainerInterceptors(final MethodIdentifier methodIdentifier, final List<InterceptorDescription> containerInterceptors) {
        this.methodLevelContainerInterceptors.put(methodIdentifier, containerInterceptors);
    }

    public Set<MethodIdentifier> getTimerMethods() {
        final Set<MethodIdentifier> methods = new HashSet<MethodIdentifier>();
        if (timeoutMethod != null) {
            methods.add(MethodIdentifier.getIdentifierForMethod(timeoutMethod));
        }
        for (Method method : scheduleMethods.keySet()) {
            methods.add(MethodIdentifier.getIdentifierForMethod(method));
        }
        return methods;
    }

    /**
     * Returns a combined map of class and method level container interceptors
     */
    public Set<InterceptorDescription> getAllContainerInterceptors() {
        if (this.allContainerInterceptors == null) {
            this.allContainerInterceptors = new HashSet<InterceptorDescription>();
            this.allContainerInterceptors.addAll(this.classLevelContainerInterceptors);
            if (!this.excludeDefaultContainerInterceptors) {
                this.allContainerInterceptors.addAll(this.defaultContainerInterceptors);
            }
            for (List<InterceptorDescription> interceptors : this.methodLevelContainerInterceptors.values()) {
                this.allContainerInterceptors.addAll(interceptors);
            }
        }
        return this.allContainerInterceptors;
    }

    public String getPolicyContextID() {
        return policyContextID;
    }

    public void setPolicyContextID(String policyContextID) {
        this.policyContextID = policyContextID;
    }

    public ShutDownInterceptorFactory getShutDownInterceptorFactory() {
        return shutDownInterceptorFactory;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "serviceName=" + getServiceName() +
                '}' + "@" + Integer.toHexString(hashCode());
    }

    public HashMap<Integer, InterceptorFactory> getElytronInterceptorFactories(String policyContextID, boolean enableJacc, boolean propagateSecurity) {
        final HashMap<Integer, InterceptorFactory> interceptorFactories = new HashMap<>(2);
        final Set<String> roles = new HashSet<>();

        // First interceptor: security domain association
        interceptorFactories.put(InterceptorOrder.View.SECURITY_CONTEXT, SecurityDomainInterceptorFactory.INSTANCE);

        if (enableJacc) {
            // Next interceptor: policy context ID
            interceptorFactories.put(InterceptorOrder.View.POLICY_CONTEXT, new ImmediateInterceptorFactory(new PolicyContextIdInterceptor(policyContextID)));
        }

        if (securityRoles != null) {
            final Map<String, Set<String>> principalVsRolesMap = securityRoles.getPrincipalVersusRolesMap();
            if (! principalVsRolesMap.isEmpty()) {
                interceptorFactories.put(InterceptorOrder.View.SECURITY_ROLES, new ImmediateInterceptorFactory(new SecurityRolesAddingInterceptor("ejb", principalVsRolesMap)));
            }
        }

        // Next interceptor: run-as-principal
        // Switch users if there's a run-as principal
        if (runAsPrincipal != null) {
            interceptorFactories.put(InterceptorOrder.View.RUN_AS_PRINCIPAL, new ImmediateInterceptorFactory(new RunAsPrincipalInterceptor(runAsPrincipal)));

            // Next interceptor: extra principal roles
            if (securityRoles != null) {
                final Set<String> extraRoles = securityRoles.getSecurityRoleNamesByPrincipal(runAsPrincipal);
                if (! extraRoles.isEmpty()) {
                    interceptorFactories.put(InterceptorOrder.View.EXTRA_PRINCIPAL_ROLES, new ImmediateInterceptorFactory(new RoleAddingInterceptor("ejb", RoleMapper.constant(Roles.fromSet(extraRoles)))));
                    roles.addAll(extraRoles);
                }
            }

        // Next interceptor: prevent identity propagation
        } else if (! propagateSecurity) {
            interceptorFactories.put(InterceptorOrder.View.RUN_AS_PRINCIPAL, new ImmediateInterceptorFactory(new RunAsPrincipalInterceptor(RunAsPrincipalInterceptor.ANONYMOUS_PRINCIPAL)));
        }

        // Next interceptor: run-as-role
        if (runAsRole != null) {
            interceptorFactories.put(InterceptorOrder.View.RUN_AS_ROLE, new ImmediateInterceptorFactory(new RoleAddingInterceptor("ejb", RoleMapper.constant(Roles.fromSet(Collections.singleton(runAsRole))))));
            roles.add(runAsRole);
        }

        // Next interceptor: security identity outflow
        if (! roles.isEmpty()) {
            interceptorFactories.put(InterceptorOrder.View.SECURITY_IDENTITY_OUTFLOW, new IdentityOutflowInterceptorFactory("ejb", RoleMapper.constant(Roles.fromSet(roles))));
        } else {
            interceptorFactories.put(InterceptorOrder.View.SECURITY_IDENTITY_OUTFLOW, IdentityOutflowInterceptorFactory.INSTANCE);
        }

        // Ignoring declared roles
        RoleMapper.constant(Roles.fromSet(getDeclaredRoles()));

        return interceptorFactories;
    }

    public void setSecurityRequired(final boolean securityRequired) {
        this.securityRequired = securityRequired;
    }

    public boolean isSecurityRequired() {
        return securityRequired;
    }

}
