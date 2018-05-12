/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static io.undertow.util.StatusCodes.OK;
import static java.security.AccessController.doPrivileged;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN;
import static org.wildfly.extension.undertow.Capabilities.REF_HTTP_AUTHENTICATION_FACTORY;
import static org.wildfly.extension.undertow.Capabilities.REF_JACC_POLICY;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;
import static org.wildfly.security.http.HttpConstants.CONFIG_CONTEXT_PATH;
import static org.wildfly.security.http.HttpConstants.CONFIG_ERROR_PAGE;
import static org.wildfly.security.http.HttpConstants.CONFIG_LOGIN_PAGE;
import static org.wildfly.security.http.HttpConstants.CONFIG_REALM;

import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.servlet.Filter;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.elytron.web.undertow.server.ElytronRunAsHandler;
import org.wildfly.elytron.web.undertow.server.ScopeSessionListener;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.jacc.JACCAuthorizationManager;
import org.wildfly.extension.undertow.security.sso.DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.AuthorizationFailureException;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.HttpScopeNotification;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.Scope;
import org.wildfly.security.http.util.PropertiesServerMechanismFactory;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnServerMechanismFactory;
import org.wildfly.security.http.util.sso.SingleSignOnServerMechanismFactory.SingleSignOnConfiguration;
import org.wildfly.security.http.util.sso.SingleSignOnSessionFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.AuthorizationManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.LifecycleInterceptor;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.SingleConstraintMatch;
import io.undertow.servlet.core.DefaultAuthorizationManager;
import io.undertow.servlet.handlers.ServletChain;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.SavedRequest;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain as specified in a web application
 * to an {@link HttpAuthenticationFactory} plus additional policy information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ApplicationSecurityDomainDefinition extends PersistentResourceDefinition {


    private static final String ANONYMOUS_PRINCIPAL = "anonymous";
    private static final String SERVLET = "servlet";
    private static final String EJB = "ejb";

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(CAPABILITY_APPLICATION_SECURITY_DOMAIN, true, BiFunction.class)
            .build();

    static final SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENITCATION_FACTORY, ModelType.STRING, false)
            .setMinSize(1)
            .setRestartAllServices()
            .setCapabilityReference(REF_HTTP_AUTHENTICATION_FACTORY)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_FACTORY_REF)
            .build();

    static final SimpleAttributeDefinition OVERRIDE_DEPLOYMENT_CONFIG = new SimpleAttributeDefinitionBuilder(Constants.OVERRIDE_DEPLOYMENT_CONFIG, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    private static final StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(Constants.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition ENABLE_JACC = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setMinSize(1)
            .setRestartAllServices()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { HTTP_AUTHENTICATION_FACTORY, OVERRIDE_DEPLOYMENT_CONFIG, ENABLE_JACC };

    static final ApplicationSecurityDomainDefinition INSTANCE = new ApplicationSecurityDomainDefinition();

    private static final Set<String> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private ApplicationSecurityDomainDefinition() {
        this((Parameters) new Parameters(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN,
                UndertowExtension.getResolver(Constants.APPLICATION_SECURITY_DOMAIN))
                        .setCapabilities(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY)
                        .addAccessConstraints(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, Constants.APPLICATION_SECURITY_DOMAIN, false, false, false)),
                                new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(UndertowExtension.SUBSYSTEM_NAME, Constants.APPLICATION_SECURITY_DOMAIN)))
                        , new AddHandler());
    }

    private ApplicationSecurityDomainDefinition(Parameters parameters, AbstractAddStepHandler add) {
        super(parameters.setAddHandler(add).setRemoveHandler(new RemoveHandler(add)));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        knownApplicationSecurityDomains.clear(); // If we are registering, time for a clean start.
        super.registerAttributes(resourceRegistration);
        if (resourceRegistration.getProcessType().isServer()) {
            resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
        }
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.singletonList(new ApplicationSecurityDomainSingleSignOnDefinition());
    }

    private static class ReferencingDeploymentsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isDefaultRequiresRuntime()) {
                context.addStep((ctx, op) -> {
                    RuntimeCapability<Void> runtimeCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(ctx.getCurrentAddressValue());
                    ServiceName applicationSecurityDomainName = runtimeCapability.getCapabilityServiceName(BiFunction.class);

                    ServiceRegistry serviceRegistry = ctx.getServiceRegistry(false);
                    ServiceController<?> controller = serviceRegistry.getRequiredService(applicationSecurityDomainName);

                    ModelNode deploymentList = new ModelNode();
                    if (controller.getState() == State.UP) {
                        Service service = controller.getService();
                        if (service instanceof ApplicationSecurityDomainService) {
                            for (String current : ((ApplicationSecurityDomainService) service).getDeployments()) {
                                deploymentList.add(current);
                            }
                        }
                    }

                    context.getResult().set(deploymentList);
                }, OperationContext.Stage.RUNTIME);
            }
        }

    }

    private static class AddHandler extends AbstractAddStepHandler {

        private AddHandler() {
            super(ATTRIBUTES);
        }

        /* (non-Javadoc)
         * @see org.jboss.as.controller.AbstractAddStepHandler#populateModel(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.as.controller.registry.Resource)
         */
        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            knownApplicationSecurityDomains.add(context.getCurrentAddressValue());
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            ModelNode model = resource.getModel();
            CapabilityServiceTarget target = context.getCapabilityServiceTarget();

            String httpServerMechanismFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model).asString();
            boolean overrideDeploymentConfig = OVERRIDE_DEPLOYMENT_CONFIG.resolveModelAttribute(context, model).asBoolean();
            boolean enableJacc = ENABLE_JACC.resolveModelAttribute(context, model).asBoolean();

            String securityDomainName = context.getCurrentAddressValue();

            ApplicationSecurityDomainService applicationSecurityDomainService = new ApplicationSecurityDomainService(overrideDeploymentConfig, enableJacc);

            CapabilityServiceBuilder<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> serviceBuilder = target
                    .addCapability(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY, applicationSecurityDomainService)
                    .setInitialMode(Mode.LAZY);

            serviceBuilder.addCapabilityRequirement(REF_HTTP_AUTHENTICATION_FACTORY, HttpAuthenticationFactory.class,
                    applicationSecurityDomainService.getHttpAuthenticationFactoryInjector(), httpServerMechanismFactory);

            if (enableJacc) {
                serviceBuilder.addCapabilityRequirement(REF_JACC_POLICY, Policy.class);
            }

            if (resource.hasChild(UndertowExtension.PATH_SSO)) {
                ModelNode ssoModel = resource.getChild(UndertowExtension.PATH_SSO).getModel();

                String cookieName = SingleSignOnDefinition.Attribute.COOKIE_NAME.resolveModelAttribute(context, ssoModel).asString();
                String domain = SingleSignOnDefinition.Attribute.DOMAIN.resolveModelAttribute(context, ssoModel).asString();
                String path = SingleSignOnDefinition.Attribute.PATH.resolveModelAttribute(context, ssoModel).asString();
                boolean httpOnly = SingleSignOnDefinition.Attribute.HTTP_ONLY.resolveModelAttribute(context, ssoModel).asBoolean();
                boolean secure = SingleSignOnDefinition.Attribute.SECURE.resolveModelAttribute(context, ssoModel).asBoolean();
                SingleSignOnConfiguration singleSignOnConfiguration = new SingleSignOnConfiguration(cookieName, domain, path, httpOnly, secure);

                ServiceName managerServiceName = new SingleSignOnManagerServiceNameProvider(securityDomainName).getServiceName();
                SessionIdGenerator generator = new SecureRandomSessionIdGenerator();

                DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider.INSTANCE
                        .map(provider -> provider.getServiceConfigurator(managerServiceName, securityDomainName, generator))
                        .orElse(new SimpleCapabilityServiceConfigurator<>(managerServiceName, new DefaultSingleSignOnManager(new ConcurrentHashMap<>(), generator::createSessionId)))
                        .configure(context).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

                ServiceConfigurator factoryConfigurator = new SingleSignOnSessionFactoryServiceConfigurator(securityDomainName).configure(context, ssoModel);
                factoryConfigurator.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

                InjectedValue<SingleSignOnSessionFactory> singleSignOnSessionFactory = new InjectedValue<>();
                serviceBuilder.addDependency(factoryConfigurator.getServiceName(), SingleSignOnSessionFactory.class, singleSignOnSessionFactory);

                applicationSecurityDomainService.getSingleSignOnSessionFactoryInjector().inject(factory -> new SingleSignOnServerMechanismFactory(factory, singleSignOnSessionFactory.getValue(), singleSignOnConfiguration));
            }

            serviceBuilder.install();
        }

    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {

        /**
         * @param addOperation
         */
        protected RemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation);
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            knownApplicationSecurityDomains.remove(context.getCurrentAddressValue());
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
            super.performRuntime(context, operation, model);
            if (context.isResourceServiceRestartAllowed()) {
                final String securityDomainName = context.getCurrentAddressValue();
                context.removeService(new SingleSignOnManagerServiceNameProvider(securityDomainName).getServiceName());
                context.removeService(new SingleSignOnSessionFactoryServiceNameProvider(securityDomainName).getServiceName());
            }
        }

        @Override
        protected ServiceName serviceName(String name) {
            RuntimeCapability<?> dynamicCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(name);
            return dynamicCapability.getCapabilityServiceName(BiFunction.class); // no-arg getCapabilityServiceName() would be fine too
        }

    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    Predicate<String> getKnownSecurityDomainPredicate() {
        return knownApplicationSecurityDomains::contains;
    }

    private static class ApplicationSecurityDomainService implements Service<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>>, BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> {

        private final boolean overrideDeploymentConfig;
        private final InjectedValue<HttpAuthenticationFactory> httpAuthenticationFactoryInjector = new InjectedValue<>();
        private final InjectedValue<UnaryOperator<HttpServerAuthenticationMechanismFactory>> singleSignOnTransformer = new InjectedValue<>();
        private final Set<RegistrationImpl> registrations = new HashSet<>();
        private final boolean enableJacc;
        private SecurityDomain securityDomain;

        private HttpAuthenticationFactory httpAuthenticationFactory;

        private ApplicationSecurityDomainService(final boolean overrideDeploymentConfig, boolean enableJacc) {
            this.overrideDeploymentConfig = overrideDeploymentConfig;
            this.enableJacc = enableJacc;
        }

        @Override
        public void start(StartContext context) throws StartException {
            httpAuthenticationFactory = httpAuthenticationFactoryInjector.getValue();
            securityDomain = httpAuthenticationFactory.getSecurityDomain();
        }

        @Override
        public void stop(StopContext context) {
            httpAuthenticationFactory = null;
        }

        @Override
        public BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }

        private Injector<HttpAuthenticationFactory> getHttpAuthenticationFactoryInjector() {
            return httpAuthenticationFactoryInjector;
        }

        Injector<UnaryOperator<HttpServerAuthenticationMechanismFactory>> getSingleSignOnSessionFactoryInjector() {
            return this.singleSignOnTransformer;
        }

        @Override
        public Registration apply(DeploymentInfo deploymentInfo, Function<String, RunAsIdentityMetaData> runAsMapper) {
            final ScopeSessionListener scopeSessionListener = ScopeSessionListener.builder()
                    .addScopeResolver(Scope.APPLICATION, ApplicationSecurityDomainService::applicationScope)
                    .build();
            if (WildFlySecurityManager.isChecking()) {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    securityDomain.registerWithClassLoader(deploymentInfo.getClassLoader());
                    return null;
                });
            } else {
                securityDomain.registerWithClassLoader(deploymentInfo.getClassLoader());
            }

            deploymentInfo.addSessionListener(scopeSessionListener);

            deploymentInfo.addInnerHandlerChainWrapper(h -> finalSecurityHandlers(h, runAsMapper));
            deploymentInfo.setInitialSecurityWrapper(h -> initialSecurityHandler(deploymentInfo, h, scopeSessionListener));
            deploymentInfo.addLifecycleInterceptor(new RunAsLifecycleInterceptor(runAsMapper));

            if (enableJacc) {
                deploymentInfo.setAuthorizationManager(new JACCAuthorizationManager());
            } else {
                deploymentInfo.setAuthorizationManager(createElytronAuthorizationManager());
            }

            RegistrationImpl registration = new RegistrationImpl(deploymentInfo);
            synchronized(registrations) {
                registrations.add(registration);
            }
            return registration;
        }

        private List<HttpServerAuthenticationMechanism> getAuthenticationMechanisms(Map<String, Map<String, String>> selectedMechanisms) {
            List<HttpServerAuthenticationMechanism> mechanisms = new ArrayList<>(selectedMechanisms.size());
            UnaryOperator<HttpServerAuthenticationMechanismFactory> singleSignOnTransformer = this.singleSignOnTransformer.getOptionalValue();
            for (Entry<String, Map<String, String>> entry : selectedMechanisms.entrySet()) {
                try {
                    UnaryOperator<HttpServerAuthenticationMechanismFactory> factoryTransformation = f -> {
                        HttpServerAuthenticationMechanismFactory factory = new PropertiesServerMechanismFactory(f, entry.getValue());
                        return (singleSignOnTransformer != null) ? singleSignOnTransformer.apply(factory) : factory;
                    };
                    HttpServerAuthenticationMechanism mechanism =  httpAuthenticationFactory.createMechanism(entry.getKey(), factoryTransformation);
                    if (mechanism != null) mechanisms.add(mechanism);
                } catch (HttpAuthenticationException e) {
                    throw new IllegalStateException(e);
                }
            }

            return mechanisms;
        }

        private HttpHandler initialSecurityHandler(final DeploymentInfo deploymentInfo, HttpHandler toWrap, ScopeSessionListener scopeSessionListener) {
            final Collection<String> availableMechanisms = httpAuthenticationFactory.getMechanismNames();
            if (availableMechanisms.isEmpty()) {
                throw ROOT_LOGGER.noMechanismsAvailable();
            }

            Map<String, String> tempBaseConfiguration = new HashMap<>();
            tempBaseConfiguration.put(CONFIG_CONTEXT_PATH, deploymentInfo.getContextPath());

            LoginConfig loginConfig = deploymentInfo.getLoginConfig();
            if (loginConfig != null) {
                String realm = loginConfig.getRealmName();
                if (realm != null) tempBaseConfiguration.put(CONFIG_REALM, realm);
                String loginPage = loginConfig.getLoginPage();
                if (loginPage != null) tempBaseConfiguration.put(CONFIG_LOGIN_PAGE, loginPage);
                String errorPage = loginConfig.getErrorPage();
                if (errorPage != null) tempBaseConfiguration.put(CONFIG_ERROR_PAGE, errorPage);
            }
            final Map<String, String> baseConfiguration = Collections.unmodifiableMap(tempBaseConfiguration);

            final Map<String, Map<String, String>> selectedMechanisms = new LinkedHashMap<>();
            if (overrideDeploymentConfig || (loginConfig == null)) {
                final Map<String, String> mechanismConfiguration = baseConfiguration;
                for (String n : availableMechanisms) {
                    selectedMechanisms.put(n, mechanismConfiguration);
                }
            } else {
                final List<AuthMethodConfig> authMethods = loginConfig.getAuthMethods();
                if (authMethods.isEmpty()) {
                    throw ROOT_LOGGER.noMechanismsSelected();
                }
                for (AuthMethodConfig c : authMethods) {
                    String name = c.getName();
                    if (availableMechanisms.contains(name) == false) {
                        throw ROOT_LOGGER.requiredMechanismNotAvailable(name, availableMechanisms);
                    }

                    Map<String, String> mechanismConfiguration;
                    Map<String, String> additionalProperties = c.getProperties();
                    if (additionalProperties != null) {
                        mechanismConfiguration = new HashMap<>(baseConfiguration);
                        mechanismConfiguration.putAll(additionalProperties);
                        mechanismConfiguration = Collections.unmodifiableMap(mechanismConfiguration);
                    } else {
                        mechanismConfiguration = baseConfiguration;
                    }
                    selectedMechanisms.put(name, mechanismConfiguration);
                }
            }

            HashMap<Scope, Function<HttpServerExchange, HttpScope>> scopeResolvers = new HashMap<>();

            scopeResolvers.put(Scope.APPLICATION, ApplicationSecurityDomainService::applicationScope);
            scopeResolvers.put(Scope.EXCHANGE, ApplicationSecurityDomainService::requestScope);
            scopeResolvers.put(Scope.SESSION, exchange -> sessionScope(exchange, scopeSessionListener));

            return ElytronContextAssociationHandler.builder()
                    .setNext(toWrap)
                    .setSecurityDomain(httpAuthenticationFactory.getSecurityDomain())
                    .setMechanismSupplier(() -> getAuthenticationMechanisms(selectedMechanisms))
                    .setHttpExchangeSupplier(httpServerExchange -> new ElytronHttpExchange(httpServerExchange, scopeResolvers, scopeSessionListener) {
                        @Override
                        protected SessionManager getSessionManager() {
                            ServletRequestContext servletRequestContext = httpServerExchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                            return servletRequestContext.getDeployment().getSessionManager();
                        }

                        @Override
                        protected SessionConfig getSessionConfig() {
                            ServletRequestContext servletRequestContext = httpServerExchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                            return servletRequestContext.getCurrentServletContext().getSessionConfig();
                        }

                        @Override
                        public int forward(String path) {
                            final ServletRequestContext servletRequestContext = httpServerExchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                            ServletRequest req = servletRequestContext.getServletRequest();
                            ServletResponse resp = servletRequestContext.getServletResponse();
                            RequestDispatcher disp = req.getRequestDispatcher(path);

                            final FormResponseWrapper respWrapper = httpServerExchange.getStatusCode() != OK && resp instanceof HttpServletResponse
                                    ? new FormResponseWrapper((HttpServletResponse) resp) : null;

                            try {
                                disp.forward(req, respWrapper != null ? respWrapper : resp);
                            } catch (ServletException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return respWrapper != null ? respWrapper.getStatus() : httpServerExchange.getStatusCode();
                        }

                        @Override
                        public boolean suspendRequest() {
                            SavedRequest.trySaveRequest(httpServerExchange);

                            return true;
                        }

                        @Override
                        public boolean resumeRequest() {
                            final ServletRequestContext servletRequestContext = httpServerExchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);

                            HttpSession session = servletRequestContext.getCurrentServletContext().getSession(httpServerExchange, false);
                            if (session != null) {
                                SavedRequest.tryRestoreRequest(httpServerExchange, session);
                            }

                            return true;
                        }

                    })
                    .build();
        }

        private static HttpScope applicationScope(HttpServerExchange exchange) {
            ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);

            if (servletRequestContext != null) {
                final Deployment deployment = servletRequestContext.getDeployment();
                final ServletContext servletContext = deployment.getServletContext();
                return new HttpScope() {
                    @Override
                    public String getID() {
                        return deployment.getDeploymentInfo().getDeploymentName();
                    }

                    @Override
                    public boolean supportsAttachments() {
                        return true;
                    }

                    @Override
                    public void setAttachment(String key, Object value) {
                        servletContext.setAttribute(key, value);
                    }

                    @Override
                    public Object getAttachment(String key) {
                        return servletContext.getAttribute(key);
                    }

                    @Override
                    public boolean supportsResources() {
                        return true;
                    }

                    @Override
                    public InputStream getResource(String path) {
                        return servletContext.getResourceAsStream(path);
                    }
                };
            }

            return null;
        }

        private static HttpScope requestScope(HttpServerExchange exchange) {
            ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);

            if (servletRequestContext != null) {
                final ServletRequest servletRequest = servletRequestContext.getServletRequest();
                return new HttpScope() {
                    @Override
                    public boolean supportsAttachments() {
                        return true;
                    }

                    @Override
                    public void setAttachment(String key, Object value) {
                        servletRequest.setAttribute(key, value);
                    }

                    @Override
                    public Object getAttachment(String key) {
                        return servletRequest.getAttribute(key);
                    }

                };
            }

            return null;
        }

        private static HttpScope sessionScope(HttpServerExchange exchange, ScopeSessionListener listener) {
            ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);

            return new HttpScope() {
                private HttpSession session = context.getOriginalRequest().getSession(false);

                @Override
                public String getID() {
                    return (exists()) ? session.getId() : null;
                }

                @Override
                public boolean exists() {
                    return session != null;
                }

                @Override
                public synchronized boolean create() {
                    if (exists()) {
                        return false;
                    }
                    session = context.getOriginalRequest().getSession(true);
                    return session != null;
                }

                @Override
                public boolean supportsAttachments() {
                    return true;
                }

                @Override
                public void setAttachment(String key, Object value) {
                    if (exists()) {
                        session.setAttribute(key, value);
                    }
                }

                @Override
                public Object getAttachment(String key) {
                    return (exists()) ? session.getAttribute(key) : null;
                }

                @Override
                public boolean supportsInvalidation() {
                    return true;
                }

                @Override
                public boolean invalidate() {
                    if (exists()) {
                        try {
                            session.invalidate();
                            return true;
                        } catch (IllegalStateException cause) {
                            // if session already invalidated we log a message and return false
                            UndertowLogger.ROOT_LOGGER.debugf("Failed to invalidate session", cause);
                        }
                    }
                    return false;
                }

                @Override
                public boolean supportsNotifications() {
                    return true;
                }

                @Override
                public void registerForNotification(Consumer<HttpScopeNotification> consumer) {
                    if (exists()) {
                        listener.registerListener(session.getId(), consumer);
                    }
                }
            };
        }

        private HttpHandler finalSecurityHandlers(HttpHandler toWrap, final Function<String, RunAsIdentityMetaData> runAsMapper) {
            return new ElytronRunAsHandler(toWrap, (s, e) -> mapIdentity(s, e, runAsMapper));
        }

        private SecurityIdentity mapIdentity(SecurityIdentity securityIdentity, HttpServerExchange exchange, Function<String, RunAsIdentityMetaData> runAsMapper) {
            final ServletChain servlet = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getCurrentServlet();

            RunAsIdentityMetaData runAsMetaData = runAsMapper.apply(servlet.getManagedServlet().getServletInfo().getName());
            return performMapping(securityIdentity, runAsMetaData);
        }

        private SecurityIdentity performMapping(SecurityIdentity securityIdentity, RunAsIdentityMetaData runAsMetaData) {
            if (runAsMetaData != null) {
                SecurityIdentity newIdentity = securityIdentity != null ? securityIdentity : securityDomain.getAnonymousSecurityIdentity();
                String runAsPrincipal = runAsMetaData.getPrincipalName();
                if (runAsPrincipal.equals(ANONYMOUS_PRINCIPAL)) {
                    try {
                        newIdentity = newIdentity.createRunAsAnonymous();
                    } catch (AuthorizationFailureException ex) {
                        newIdentity = newIdentity.createRunAsAnonymous(false);
                    }
                } else {
                    if (! runAsPrincipalExists(securityDomain, runAsPrincipal)) {
                        newIdentity = securityDomain.createAdHocIdentity(runAsPrincipal);
                    } else {
                        try {
                            newIdentity = newIdentity.createRunAsIdentity(runAsPrincipal);
                        } catch (AuthorizationFailureException ex) {
                            newIdentity = newIdentity.createRunAsIdentity(runAsPrincipal, false);
                        }
                    }
                }

                final Set<String> runAsRoleNames = new HashSet<>(runAsMetaData.getRunAsRoles().size());
                runAsRoleNames.add(runAsMetaData.getRoleName());
                runAsRoleNames.addAll(runAsMetaData.getRunAsRoles());

                RoleMapper runAsRoleMaper = RoleMapper.constant(Roles.fromSet(runAsRoleNames));

                Roles servletRoles = newIdentity.getRoles(SERVLET);
                newIdentity = newIdentity.withRoleMapper(SERVLET, runAsRoleMaper.or((roles) -> servletRoles));

                Roles ejbRoles = newIdentity.getRoles(EJB);
                newIdentity = newIdentity.withRoleMapper(EJB, runAsRoleMaper.or((roles) -> ejbRoles));

                return newIdentity;
            }

            return securityIdentity;
        }

        private boolean runAsPrincipalExists(final SecurityDomain securityDomain, final String runAsPrincipal) {
            RealmIdentity realmIdentity = null;
            try {
                realmIdentity = securityDomain.getIdentity(runAsPrincipal);
                return realmIdentity.exists();
            } catch (RealmUnavailableException e) {
                throw UndertowLogger.ROOT_LOGGER.unableToObtainIdentity(runAsPrincipal, e);
            } finally {
                if (realmIdentity != null) {
                    realmIdentity.dispose();
                }
            }
        }

        private AuthorizationManager createElytronAuthorizationManager() {
            return new AuthorizationManager() {
                @Override
                public boolean isUserInRole(String roleName, Account account, ServletInfo servletInfo, HttpServletRequest request, Deployment deployment) {
                    return DefaultAuthorizationManager.INSTANCE.isUserInRole(roleName, account, servletInfo, request, deployment);
                }

                @Override
                public boolean canAccessResource(List<SingleConstraintMatch> mappedConstraints, Account account, ServletInfo servletInfo, HttpServletRequest request, Deployment deployment) {
                    if (DefaultAuthorizationManager.INSTANCE.canAccessResource(mappedConstraints, account, servletInfo, request, deployment)) {
                        return true;
                    }

                    SecurityDomain securityDomain = httpAuthenticationFactory.getSecurityDomain();
                    SecurityIdentity securityIdentity = securityDomain.getCurrentSecurityIdentity();

                    if (securityIdentity == null) {
                        return false;
                    }

                    List<Permission> permissions = new ArrayList<>();

                    permissions.add(new WebResourcePermission(getCanonicalURI(request), request.getMethod()));

                    for (String roleName : securityIdentity.getRoles("web", true)) {
                        permissions.add(new WebRoleRefPermission(getCanonicalURI(request), roleName));
                    }

                    for (Permission permission : permissions) {
                        if (securityIdentity.implies(permission)) {
                            return true;
                        }
                    }

                    return false;
                }

                @Override
                public io.undertow.servlet.api.TransportGuaranteeType transportGuarantee(io.undertow.servlet.api.TransportGuaranteeType currentConnectionGuarantee, io.undertow.servlet.api.TransportGuaranteeType configuredRequiredGuarantee, HttpServletRequest request) {
                    return DefaultAuthorizationManager.INSTANCE.transportGuarantee(currentConnectionGuarantee, configuredRequiredGuarantee, request);
                }

                private String getCanonicalURI(HttpServletRequest request) {
                    String canonicalURI = request.getRequestURI().substring(request.getContextPath().length());
                    if (canonicalURI == null || canonicalURI.equals("/"))
                        canonicalURI = "";
                    return canonicalURI;
                }
            };
        }

        private List<String> getDeployments() {
            synchronized (registrations) {
                List<String> deployments = new ArrayList<>(registrations.size());
                for (RegistrationImpl registration : registrations) {
                    deployments.add(registration.deploymentInfo.getDeploymentName());
                }
                return deployments;
            }
        }

        private class RunAsLifecycleInterceptor implements LifecycleInterceptor {

            private final Function<String, RunAsIdentityMetaData> runAsMapper;

            RunAsLifecycleInterceptor(Function<String, RunAsIdentityMetaData> runAsMapper) {
                this.runAsMapper = runAsMapper;
            }

            private void doIt(ServletInfo servletInfo, LifecycleContext context) throws ServletException {
                RunAsIdentityMetaData runAsMetaData = runAsMapper.apply(servletInfo.getName());

                if (runAsMetaData != null) {
                    SecurityIdentity securityIdentity = performMapping(securityDomain.getAnonymousSecurityIdentity(), runAsMetaData);
                    try {
                        securityIdentity.runAs((PrivilegedExceptionAction<Void>) () -> {
                            context.proceed();
                            return null;
                        });
                    } catch (PrivilegedActionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof ServletException) {
                            throw (ServletException) cause;
                        }
                        throw new ServletException(cause);
                    }
                } else {
                    context.proceed();
                }
            }

            @Override
            public void init(ServletInfo servletInfo, Servlet servlet, LifecycleContext context) throws ServletException {
                doIt(servletInfo, context);
            }

            @Override
            public void init(FilterInfo filterInfo, Filter filter, LifecycleContext context) throws ServletException {
                context.proceed();
            }

            @Override
            public void destroy(ServletInfo servletInfo, Servlet servlet, LifecycleContext context) throws ServletException {
                doIt(servletInfo, context);
            }

            @Override
            public void destroy(FilterInfo filterInfo, Filter filter, LifecycleContext context) throws ServletException {
                context.proceed();
            }

        }

        private class RegistrationImpl implements Registration {

            final DeploymentInfo deploymentInfo;

            private RegistrationImpl(DeploymentInfo deploymentInfo) {
                this.deploymentInfo = deploymentInfo;
            }

            @Override
            public void cancel() {
                if (WildFlySecurityManager.isChecking()) {
                    doPrivileged((PrivilegedAction<Void>) () -> {
                        SecurityDomain.unregisterClassLoader(deploymentInfo.getClassLoader());
                        return null;
                    });
                } else {
                    SecurityDomain.unregisterClassLoader(deploymentInfo.getClassLoader());
                }
                synchronized(registrations) {
                    registrations.remove(this);
                }
            }

        }

    }

    public interface Registration {

        /**
         * Cancel the registration.
         */
        void cancel();

    }

    private static class FormResponseWrapper extends HttpServletResponseWrapper {

        private int status = OK;

        private FormResponseWrapper(final HttpServletResponse wrapped) {
            super(wrapped);
        }

        @Override
        public void setStatus(int sc, String sm) {
            status = sc;
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
        }

        @Override
        public int getStatus() {
            return status;
        }

    }


}
