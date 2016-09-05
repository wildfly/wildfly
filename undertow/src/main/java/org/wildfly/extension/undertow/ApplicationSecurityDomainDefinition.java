/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;
import static org.wildfly.security.http.HttpConstants.CONFIG_CONTEXT_PATH;
import static org.wildfly.security.http.HttpConstants.CONFIG_ERROR_PAGE;
import static org.wildfly.security.http.HttpConstants.CONFIG_LOGIN_PAGE;
import static org.wildfly.security.http.HttpConstants.CONFIG_REALM;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.elytron.web.undertow.server.ElytronRunAsHandler;
import org.wildfly.elytron.web.undertow.server.ScopeSessionListener;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.Scope;
import org.wildfly.security.http.util.PropertiesServerMechanismFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.SavedRequest;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain as specified in a web application
 * to an {@link HttpAuthenticationFactory} plus additional policy information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ApplicationSecurityDomainDefinition extends PersistentResourceDefinition {

    public static final String APPLICATION_SECURITY_DOMAIN_CAPABILITY = "org.wildfly.extension.undertow.application-security-domain";

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(APPLICATION_SECURITY_DOMAIN_CAPABILITY, true, Function.class)
            .build();

    private static final String HTTP_AUTHENITCATION_FACTORY_CAPABILITY = "org.wildfly.security.http-authentication-factory";

    static SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENITCATION_FACTORY, ModelType.STRING, false)
            .setMinSize(1)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setCapabilityReference(HTTP_AUTHENITCATION_FACTORY_CAPABILITY, APPLICATION_SECURITY_DOMAIN_CAPABILITY, true)
            .build();

    static SimpleAttributeDefinition OVERRIDE_DEPLOYMENT_CONFIG = new SimpleAttributeDefinitionBuilder(Constants.OVERRIDE_DEPLOYMENT_CONFIG, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private static StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(Constants.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { HTTP_AUTHENTICATION_FACTORY, OVERRIDE_DEPLOYMENT_CONFIG };

    static final ApplicationSecurityDomainDefinition INSTANCE = new ApplicationSecurityDomainDefinition();

    private static final Set<String> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private ApplicationSecurityDomainDefinition() {
        this(new Parameters(PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN), UndertowExtension.getResolver(Constants.APPLICATION_SECURITY_DOMAIN))
                .setCapabilities(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY), new AddHandler());
    }

    private ApplicationSecurityDomainDefinition(Parameters parameters, AbstractAddStepHandler add) {
        super(parameters.setAddHandler(add).setRemoveHandler(new RemoveHandler(add)));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        knownApplicationSecurityDomains.clear(); // If we are registering, time for a clean start.
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
    }

    private static class ReferencingDeploymentsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            RuntimeCapability<Void> runtimeCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName applicationSecurityDomainName = runtimeCapability.getCapabilityServiceName(Function.class);

            ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
            ServiceController<?> controller = serviceRegistry.getRequiredService(applicationSecurityDomainName);

            ModelNode deploymentList = new ModelNode();
            if (controller.getState() == State.UP) {
                Service service = controller.getService();
                if (service instanceof ApplicationSecurityDomainService) {
                    for (String current : ((ApplicationSecurityDomainService)service).getDeployments()) {
                        deploymentList.add(current);
                    }
                }
            }

            context.getResult().set(deploymentList);
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
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

            String httpServerMechanismFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model).asString();
            boolean overrideDeploymentConfig = OVERRIDE_DEPLOYMENT_CONFIG.resolveModelAttribute(context, model).asBoolean();

            RuntimeCapability<?> runtimeCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName serviceName = runtimeCapability.getCapabilityServiceName(Function.class);

            ApplicationSecurityDomainService applicationSecurityDomainService = new ApplicationSecurityDomainService(overrideDeploymentConfig);

            ServiceBuilder<Function<DeploymentInfo, Registration>> serviceBuilder = context.getServiceTarget().addService(serviceName, applicationSecurityDomainService)
                    .setInitialMode(Mode.LAZY);

            serviceBuilder.addDependency(context.getCapabilityServiceName(HTTP_AUTHENITCATION_FACTORY_CAPABILITY,
                    httpServerMechanismFactory, HttpAuthenticationFactory.class), HttpAuthenticationFactory.class, applicationSecurityDomainService.getHttpAuthenticationFactoryInjector());

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
        protected ServiceName serviceName(String name) {
            RuntimeCapability<?> dynamicCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(name);
            return dynamicCapability.getCapabilityServiceName(HttpAuthenticationFactory.class);
        }

    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    Predicate<String> getKnownSecurityDomainPredicate() {
        return knownApplicationSecurityDomains::contains;
    }

    private static class ApplicationSecurityDomainService implements Service<Function<DeploymentInfo, Registration>> {

        private final boolean overrideDeploymentConfig;
        private final InjectedValue<HttpAuthenticationFactory> httpAuthenticationFactoryInjector = new InjectedValue<>();
        private final Set<RegistrationImpl> registrations = new HashSet<>();

        private HttpAuthenticationFactory httpAuthenticationFactory;

        private ApplicationSecurityDomainService(final boolean overrideDeploymentConfig) {
            this.overrideDeploymentConfig = overrideDeploymentConfig;
        }

        @Override
        public void start(StartContext context) throws StartException {
            httpAuthenticationFactory = httpAuthenticationFactoryInjector.getValue();
        }

        @Override
        public void stop(StopContext context) {
            httpAuthenticationFactory = null;
        }

        @Override
        public Function<DeploymentInfo, Registration> getValue() throws IllegalStateException, IllegalArgumentException {
            return this::applyElytronSecurity;
        }

        private Injector<HttpAuthenticationFactory> getHttpAuthenticationFactoryInjector() {
            return httpAuthenticationFactoryInjector;
        }

        private Registration applyElytronSecurity(final DeploymentInfo deploymentInfo) {
            final ScopeSessionListener scopeSessionListener = ScopeSessionListener.builder()
                    .addScopeResolver(Scope.APPLICATION, ApplicationSecurityDomainService::applicationScope)
                    .build();
            deploymentInfo.addSessionListener(scopeSessionListener);

            deploymentInfo.addInnerHandlerChainWrapper(this::finalSecurityHandlers);
            deploymentInfo.setInitialSecurityWrapper(h -> initialSecurityHandler(deploymentInfo, h, scopeSessionListener));

            RegistrationImpl registration = new RegistrationImpl(deploymentInfo);
            synchronized(registrations) {
                registrations.add(registration);
            }
            return registration;
        }

        private List<HttpServerAuthenticationMechanism> getAuthenticationMechanisms(Map<String, Map<String, String>> selectedMechanisms) {
            List<HttpServerAuthenticationMechanism> mechanisms = new ArrayList<>(selectedMechanisms.size());
            selectedMechanisms.forEach((n, c) -> {
                try {
                    HttpServerAuthenticationMechanism mechanism =  httpAuthenticationFactory.createMechanism(n, (f) -> new PropertiesServerMechanismFactory(f, c));
                    if (mechanism!= null) mechanisms.add(mechanism);
                } catch (HttpAuthenticationException e) {
                    throw new IllegalStateException(e);
                }
            });

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
                availableMechanisms.forEach(n -> selectedMechanisms.put(n, mechanismConfiguration));
            } else {
                final List<AuthMethodConfig> authMethods = loginConfig.getAuthMethods();
                if (authMethods.isEmpty()) {
                    throw ROOT_LOGGER.noMechanismsSelected();
                }
                authMethods.forEach(c -> {
                    String name = c.getName();
                    if (availableMechanisms.contains(name) == false) {
                        throw ROOT_LOGGER.requiredMechanismNotAvailable(name);
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
                });
            }

            HashMap<Scope, Function<HttpServerExchange, HttpScope>> scopeResolvers = new HashMap<>();

            scopeResolvers.put(Scope.APPLICATION, ApplicationSecurityDomainService::applicationScope);
            scopeResolvers.put(Scope.EXCHANGE, ApplicationSecurityDomainService::requestScope);

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
                final ServletContext servletContext = servletRequestContext.getDeployment().getServletContext();
                return new HttpScope() {

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

        private HttpHandler finalSecurityHandlers(HttpHandler toWrap) {
            return new BlockingHandler(new ElytronRunAsHandler(toWrap));
        }

        private String[] getDeployments() {
            synchronized(registrations) {
                return registrations.stream().map(r -> r.deploymentInfo.getDeploymentName()).collect(Collectors.toList()).toArray(new String[registrations.size()]);
            }
        }

        private class RegistrationImpl implements Registration {

            private final DeploymentInfo deploymentInfo;

            private RegistrationImpl(DeploymentInfo deploymentInfo) {
                this.deploymentInfo = deploymentInfo;
            }

            @Override
            public void cancel() {
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
