/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static java.security.AccessController.doPrivileged;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS;
import static org.wildfly.extension.undertow.Capabilities.REF_HTTP_AUTHENTICATION_FACTORY;
import static org.wildfly.extension.undertow.Capabilities.REF_SECURITY_DOMAIN;
import static org.wildfly.security.http.HttpConstants.BASIC_NAME;
import static org.wildfly.security.http.HttpConstants.CLIENT_CERT_NAME;
import static org.wildfly.security.http.HttpConstants.DIGEST_NAME;
import static org.wildfly.security.http.HttpConstants.FORM_NAME;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.AttachmentKey;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
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
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.elytron.web.undertow.server.servlet.AuthenticationManager;
import org.wildfly.extension.undertow.security.jacc.JACCAuthorizationManager;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.MechanismConfiguration;
import org.wildfly.security.auth.server.MechanismConfigurationSelector;
import org.wildfly.security.auth.server.MechanismRealmConfiguration;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.cache.IdentityCache;
import org.wildfly.security.http.HttpExchangeSpi;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.basic.BasicMechanismFactory;
import org.wildfly.security.http.bearer.BearerMechanismFactory;
import org.wildfly.security.http.cert.ClientCertMechanismFactory;
import org.wildfly.security.http.digest.DigestMechanismFactory;
import org.wildfly.security.http.external.ExternalMechanismFactory;
import org.wildfly.security.http.form.FormMechanismFactory;
import org.wildfly.security.http.spnego.SpnegoMechanismFactory;
import org.wildfly.security.http.util.AggregateServerMechanismFactory;
import org.wildfly.security.http.util.FilterServerMechanismFactory;
import org.wildfly.security.http.util.sso.ProgrammaticSingleSignOnCache;
import org.wildfly.security.http.util.sso.SingleSignOnServerMechanismFactory;
import org.wildfly.security.http.util.sso.SingleSignOnConfiguration;
import org.wildfly.security.http.util.sso.SingleSignOnSessionFactory;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain as specified in a web application
 * to an {@link HttpAuthenticationFactory} plus additional policy information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ApplicationSecurityDomainDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN);
    private static final Predicate<String> SERVLET_MECHANISM;

    static {
        Set<String> defaultMechanisms = new HashSet<>(4);
        defaultMechanisms.add(BASIC_NAME);
        defaultMechanisms.add(CLIENT_CERT_NAME);
        defaultMechanisms.add(DIGEST_NAME);
        defaultMechanisms.add(FORM_NAME);

        SERVLET_MECHANISM = defaultMechanisms::contains;
    }

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(CAPABILITY_APPLICATION_SECURITY_DOMAIN, true, BiFunction.class)
            .build();

    static final SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENTICATION_FACTORY, ModelType.STRING, false)
            .setMinSize(1)
            .setRestartAllServices()
            .setCapabilityReference(REF_HTTP_AUTHENTICATION_FACTORY)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_FACTORY_REF)
            .setAlternatives(Constants.SECURITY_DOMAIN)
            .build();

    static final SimpleAttributeDefinition OVERRIDE_DEPLOYMENT_CONFIG = new SimpleAttributeDefinitionBuilder(Constants.OVERRIDE_DEPLOYMENT_CONFIG, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .setRequires(Constants.HTTP_AUTHENTICATION_FACTORY)
            .build();

    static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_DOMAIN, ModelType.STRING, false)
            .setMinSize(1)
            .setRestartAllServices()
            .setCapabilityReference(REF_SECURITY_DOMAIN)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .setAlternatives(Constants.HTTP_AUTHENTICATION_FACTORY)
            .build();

    private static final StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(Constants.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition ENABLE_JACC = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setMinSize(1)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition ENABLE_JASPI = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_JASPI, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition INTEGRATED_JASPI = new SimpleAttributeDefinitionBuilder(Constants.INTEGRATED_JASPI, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(SECURITY_DOMAIN, HTTP_AUTHENTICATION_FACTORY, OVERRIDE_DEPLOYMENT_CONFIG, ENABLE_JACC, ENABLE_JASPI, INTEGRATED_JASPI);

    private static final AttachmentKey<KnownDeploymentsApi> KNOWN_DEPLOYMENTS_KEY = AttachmentKey.create(KnownDeploymentsApi.class);

    private final Set<String> knownApplicationSecurityDomains;
    private final ResourceOperationRuntimeHandler handler;

    ApplicationSecurityDomainDefinition(Set<String> knownApplicationSecurityDomains) {
        this(knownApplicationSecurityDomains, new AddHandler(knownApplicationSecurityDomains));
    }

    private ApplicationSecurityDomainDefinition(Set<String> knownApplicationSecurityDomains, AddHandler addHandler) {
        this(knownApplicationSecurityDomains, addHandler, new RemoveHandler(knownApplicationSecurityDomains, addHandler));
    }

    private ApplicationSecurityDomainDefinition(Set<String> knownApplicationSecurityDomains, AddHandler addHandler, RemoveHandler removeHandler) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setCapabilities(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY)
                .addAccessConstraints(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, Constants.APPLICATION_SECURITY_DOMAIN, false, false, false)),
                        new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(UndertowExtension.SUBSYSTEM_NAME, Constants.APPLICATION_SECURITY_DOMAIN)))
                .setAddHandler(addHandler)
                .setRemoveHandler(removeHandler)
        );
        this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        this.handler = new ResourceOperationRuntimeHandler() {
            @Override
            public void addRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
                addHandler.performRuntime(context, null, model);
            }

            @Override
            public void removeRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
                removeHandler.performRemove(context, null, model);
            }
        };
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
        if (resourceRegistration.getProcessType().isServer()) {
            resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        new ApplicationSecurityDomainSingleSignOnDefinition(this.handler).register(resourceRegistration, null);
    }

    private static class ReferencingDeploymentsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isDefaultRequiresRuntime()) {
                context.addStep((ctx, op) -> {
                    final KnownDeploymentsApi knownDeploymentsApi = context.getCapabilityRuntimeAPI(
                            CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, ctx.getCurrentAddressValue(),
                            KnownDeploymentsApi.class);

                    ModelNode deploymentList = new ModelNode();
                    for (String current : knownDeploymentsApi.getKnownDeployments()) {
                        deploymentList.add(current);
                    }

                    context.getResult().set(deploymentList);
                }, OperationContext.Stage.RUNTIME);
            }
        }

    }

    static class AddHandler extends AbstractAddStepHandler {
        private final Set<String> knownApplicationSecurityDomains;

        private AddHandler(Set<String> knownApplicationSecurityDomains) {
            this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        }

        /* (non-Javadoc)
         * @see org.jboss.as.controller.AbstractAddStepHandler#populateModel(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.as.controller.registry.Resource)
         */
        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            this.knownApplicationSecurityDomains.add(context.getCurrentAddressValue());
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            KnownDeploymentsApi knownDeployments = new KnownDeploymentsApi();
            context.registerCapability(RuntimeCapability.Builder
                    .of(CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, true, knownDeployments).build()
                    .fromBaseCapability(context.getCurrentAddressValue()));
            context.attach(KNOWN_DEPLOYMENTS_KEY, knownDeployments);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            CapabilityServiceTarget target = context.getCapabilityServiceTarget();

            final String securityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
            final String httpServerMechanismFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model).asStringOrNull();
            boolean overrideDeploymentConfig = OVERRIDE_DEPLOYMENT_CONFIG.resolveModelAttribute(context, model).asBoolean();
            boolean enableJacc = ENABLE_JACC.resolveModelAttribute(context, model).asBoolean();
            boolean enableJaspi = ENABLE_JASPI.resolveModelAttribute(context, model).asBoolean();
            boolean integratedJaspi = INTEGRATED_JASPI.resolveModelAttribute(context, model).asBoolean();

            String securityDomainName = context.getCurrentAddressValue();

            ServiceName securityDomainServiceName = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress()).append(Constants.SECURITY_DOMAIN);

            CapabilityServiceBuilder<?> serviceBuilder = target.addCapability(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY).setInitialMode(Mode.ON_DEMAND);

            final Supplier<HttpAuthenticationFactory> httpAuthenticationFactorySupplier;
            final Supplier<SecurityDomain> securityDomainSupplier;
            if (httpServerMechanismFactory != null) {
                httpAuthenticationFactorySupplier = serviceBuilder.requires(context.getCapabilityServiceName(REF_HTTP_AUTHENTICATION_FACTORY, HttpAuthenticationFactory.class, httpServerMechanismFactory));
                securityDomainSupplier = null;
            } else {
                securityDomainSupplier = serviceBuilder.requires(context.getCapabilityServiceName(REF_SECURITY_DOMAIN, SecurityDomain.class, securityDomain));
                httpAuthenticationFactorySupplier = null;
            }

            UnaryOperator<HttpServerAuthenticationMechanismFactory> transformer = UnaryOperator.identity();
            BiFunction<HttpExchangeSpi, String, IdentityCache> identityCacheSupplier = null;
            if (context.hasOptionalCapability(ApplicationSecurityDomainSingleSignOnDefinition.SSO_SESSION_FACTORY, securityDomainName, APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY, null)) {
                Supplier<SingleSignOnConfiguration> singleSignOnConfiguration = serviceBuilder.requires(ApplicationSecurityDomainSingleSignOnDefinition.SSO_CONFIGURATION, securityDomainName);
                Supplier<SingleSignOnSessionFactory> singleSignOnSessionFactorySupplier = serviceBuilder.requires(ApplicationSecurityDomainSingleSignOnDefinition.SSO_SESSION_FACTORY, securityDomainName);
                transformer = new UnaryOperator<>() {
                    @Override
                    public HttpServerAuthenticationMechanismFactory apply(HttpServerAuthenticationMechanismFactory factory) {
                        return new SingleSignOnServerMechanismFactory(factory, singleSignOnSessionFactorySupplier.get(), singleSignOnConfiguration.get());
                    }
                };

                identityCacheSupplier = new BiFunction<>() {
                    @Override
                    public IdentityCache apply(HttpExchangeSpi httpExchangeSpi, String mechanismName) {
                        return ProgrammaticSingleSignOnCache.newInstance(httpExchangeSpi, mechanismName, singleSignOnSessionFactorySupplier.get(), singleSignOnConfiguration.get());
                    }
                };
            }

            Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> deploymentConsumer = serviceBuilder.provides(APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY);
            Consumer<SecurityDomain> securityDomainConsumer = serviceBuilder.provides(securityDomainServiceName);
            ApplicationSecurityDomainService service = new ApplicationSecurityDomainService(overrideDeploymentConfig,
                    enableJacc, enableJaspi, integratedJaspi, httpAuthenticationFactorySupplier, securityDomainSupplier,
                    transformer, identityCacheSupplier, deploymentConsumer, securityDomainConsumer);
            serviceBuilder.setInstance(service);
            serviceBuilder.install();

            KnownDeploymentsApi knownDeploymentsApi = context.getAttachment(KNOWN_DEPLOYMENTS_KEY);
            knownDeploymentsApi.setApplicationSecurityDomainService(service);
        }
    }

    private static HttpAuthenticationFactory toHttpAuthenticationFactory(final SecurityDomain securityDomain, final String realmName) {
        final HttpServerAuthenticationMechanismFactory mechanismFactory = new FilterServerMechanismFactory(
                new AggregateServerMechanismFactory(new BasicMechanismFactory(), new BearerMechanismFactory(),
                        new ClientCertMechanismFactory(), new DigestMechanismFactory(), new ExternalMechanismFactory(),
                        new FormMechanismFactory(), new SpnegoMechanismFactory()), SERVLET_MECHANISM);
        ROOT_LOGGER.debugf("Security realm applied is \"%s\"", realmName);
        return HttpAuthenticationFactory.builder().setFactory(mechanismFactory)
                .setSecurityDomain(securityDomain)
                .setMechanismConfigurationSelector(
                        MechanismConfigurationSelector.constantSelector(realmName == null ? MechanismConfiguration.EMPTY
                                : MechanismConfiguration.builder()
                                        .addMechanismRealm(
                                                MechanismRealmConfiguration.builder().setRealmName(realmName).build())
                                        .build()))
                .build();
    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {
        private final Set<String> knownApplicationSecurityDomains;

        /**
         * @param knownApplicationSecurityDomains set from which the name of the application security domain should be removed.
         * @param addOperation  the add operation handler to use to rollback service removal. Cannot be @{code null}
         */
        protected RemoveHandler(Set<String> knownApplicationSecurityDomains, AbstractAddStepHandler addOperation) {
            super(addOperation);
            this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            this.knownApplicationSecurityDomains.remove(context.getCurrentAddressValue());
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            context.deregisterCapability(
                    RuntimeCapability.buildDynamicCapabilityName(CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, context.getCurrentAddressValue())
            );
        }

        @Override
        protected ServiceName serviceName(String name) {
            RuntimeCapability<?> dynamicCapability = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(name);
            return dynamicCapability.getCapabilityServiceName(BiFunction.class); // no-arg getCapabilityServiceName() would be fine too
        }

    }

    Predicate<String> getKnownSecurityDomainPredicate() {
        return knownApplicationSecurityDomains::contains;
    }

    private static class ApplicationSecurityDomainService implements Service, BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> {

        private final Supplier<HttpAuthenticationFactory> httpAuthenticationFactorySupplier;
        private final Supplier<SecurityDomain> securityDomainSupplier;
        private final UnaryOperator<HttpServerAuthenticationMechanismFactory> singleSignOnTransformer;
        private final BiFunction<HttpExchangeSpi, String, IdentityCache> identityCacheSupplier;

        private final Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> deploymentConsumer;
        private final Consumer<SecurityDomain> securityDomainConsumer;

        private final boolean overrideDeploymentConfig;
        private final Set<RegistrationImpl> registrations = new HashSet<>();
        private final boolean enableJacc;
        private final boolean enableJaspi;
        private final boolean integratedJaspi;

        private volatile HttpAuthenticationFactory httpAuthenticationFactory;
        private volatile SecurityDomain securityDomain;

        private ApplicationSecurityDomainService(final boolean overrideDeploymentConfig, boolean enableJacc, boolean enableJaspi, boolean integratedJaspi,
                final Supplier<HttpAuthenticationFactory> httpAuthenticationFactorySupplier, final Supplier<SecurityDomain> securityDomainSupplier,
                UnaryOperator<HttpServerAuthenticationMechanismFactory> singleSignOnTransformer, BiFunction<HttpExchangeSpi, String, IdentityCache> identityCacheSupplier,
                Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> deploymentConsumer, Consumer<SecurityDomain> securityDomainConsumer) {
            this.overrideDeploymentConfig = overrideDeploymentConfig;
            this.enableJacc = enableJacc;
            this.enableJaspi = enableJaspi;
            this.integratedJaspi = integratedJaspi;
            this.httpAuthenticationFactorySupplier = httpAuthenticationFactorySupplier;
            this.securityDomainSupplier = securityDomainSupplier;
            this.singleSignOnTransformer = singleSignOnTransformer;
            this.identityCacheSupplier = identityCacheSupplier;
            this.deploymentConsumer = deploymentConsumer;
            this.securityDomainConsumer = securityDomainConsumer;
        }

        @Override
        public void start(StartContext context) throws StartException {
            deploymentConsumer.accept(this);
            if (httpAuthenticationFactorySupplier != null) {
                httpAuthenticationFactory = httpAuthenticationFactorySupplier.get();
                securityDomain = httpAuthenticationFactory.getSecurityDomain();
            } else {
                securityDomain = securityDomainSupplier.get();
            }
            securityDomainConsumer.accept(securityDomain);
        }

        @Override
        public void stop(StopContext context) {}

        @Override
        public Registration apply(DeploymentInfo deploymentInfo, Function<String, RunAsIdentityMetaData> runAsMapper) {
            HttpAuthenticationFactory httpAuthenticationFactory = this.httpAuthenticationFactory != null
                    ? this.httpAuthenticationFactory
                    : toHttpAuthenticationFactory(securityDomain, getRealmName(deploymentInfo));
            AuthenticationManager.Builder builder = AuthenticationManager.builder()
                    .setHttpAuthenticationFactory(httpAuthenticationFactory)
                    .setOverrideDeploymentConfig(overrideDeploymentConfig)
                    .setHttpAuthenticationFactoryTransformer(singleSignOnTransformer)
                    .setIdentityCacheSupplier(identityCacheSupplier)
                    .setRunAsMapper(runAsMapper)
                    .setEnableJaspi(enableJaspi)
                    .setIntegratedJaspi(integratedJaspi);

            if (enableJacc) {
                builder.setAuthorizationManager(JACCAuthorizationManager.INSTANCE);
            }

            AuthenticationManager authenticationManager = builder.build();
            authenticationManager.configure(deploymentInfo);

            RegistrationImpl registration = new RegistrationImpl(deploymentInfo);
            synchronized(registrations) {
                registrations.add(registration);
            }
            return registration;
        }

        private String getRealmName(final DeploymentInfo deploymentInfo) {
            LoginConfig loginConfig = deploymentInfo.getLoginConfig();
            return loginConfig != null ? loginConfig.getRealmName() : null;
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

    private static class KnownDeploymentsApi {

        private volatile ApplicationSecurityDomainService service;

        List<String> getKnownDeployments() {
            return service != null ? service.getDeployments() : Collections.emptyList();

        }

        void setApplicationSecurityDomainService(final ApplicationSecurityDomainService service) {
            this.service = service;
        }
    }

}
