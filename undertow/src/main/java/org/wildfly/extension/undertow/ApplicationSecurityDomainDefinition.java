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

import static java.security.AccessController.doPrivileged;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS;
import static org.wildfly.extension.undertow.Capabilities.REF_HTTP_AUTHENTICATION_FACTORY;
import static org.wildfly.extension.undertow.Capabilities.REF_JACC_POLICY;

import static org.wildfly.extension.undertow.Capabilities.REF_SECURITY_DOMAIN;
import static org.wildfly.security.http.HttpConstants.BASIC_NAME;
import static org.wildfly.security.http.HttpConstants.CLIENT_CERT_NAME;
import static org.wildfly.security.http.HttpConstants.DIGEST_NAME;
import static org.wildfly.security.http.HttpConstants.FORM_NAME;

import java.security.Policy;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.AttachmentKey;
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
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.elytron.web.undertow.server.servlet.AuthenticationManager;
import org.wildfly.extension.undertow.security.jacc.JACCAuthorizationManager;
import org.wildfly.extension.undertow.security.sso.DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;

import org.wildfly.security.auth.server.MechanismConfiguration;
import org.wildfly.security.auth.server.MechanismConfigurationSelector;
import org.wildfly.security.auth.server.MechanismRealmConfiguration;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.impl.ServerMechanismFactoryImpl;
import org.wildfly.security.http.util.FilterServerMechanismFactory;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnServerMechanismFactory;
import org.wildfly.security.http.util.sso.SingleSignOnServerMechanismFactory.SingleSignOnConfiguration;
import org.wildfly.security.http.util.sso.SingleSignOnSessionFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain as specified in a web application
 * to an {@link HttpAuthenticationFactory} plus additional policy information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ApplicationSecurityDomainDefinition extends PersistentResourceDefinition {

    private static Predicate<String> SERVLET_MECHANISM;

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

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS_CAPABILITY = RuntimeCapability
            .Builder.of(CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, true)
            .build();

    static final SimpleAttributeDefinition HTTP_AUTHENTICATION_FACTORY = new SimpleAttributeDefinitionBuilder(Constants.HTTP_AUTHENITCATION_FACTORY, ModelType.STRING, false)
            .setMinSize(1)
            .setRestartAllServices()
            .setCapabilityReference(REF_HTTP_AUTHENTICATION_FACTORY)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_FACTORY_REF)
            .setAlternatives(Constants.SECURITY_DOMAIN)
            .build();

    static final SimpleAttributeDefinition OVERRIDE_DEPLOYMENT_CONFIG = new SimpleAttributeDefinitionBuilder(Constants.OVERRIDE_DEPLOYMENT_CONFIG, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .setRequires(Constants.HTTP_AUTHENITCATION_FACTORY)
            .build();

    static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_DOMAIN, ModelType.STRING, false)
            .setMinSize(1)
            .setRestartAllServices()
            .setCapabilityReference(REF_SECURITY_DOMAIN)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .setAlternatives(Constants.HTTP_AUTHENITCATION_FACTORY)
            .build();

    private static final StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(Constants.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition ENABLE_JACC = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setMinSize(1)
            .setRestartAllServices()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { SECURITY_DOMAIN, HTTP_AUTHENTICATION_FACTORY, OVERRIDE_DEPLOYMENT_CONFIG, ENABLE_JACC };

    static final ApplicationSecurityDomainDefinition INSTANCE = new ApplicationSecurityDomainDefinition();

    private static final Set<String> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private static final AttachmentKey<KnownDeploymentsApi> KNOWN_DEPLOYMENTS_KEY = AttachmentKey.create(KnownDeploymentsApi.class);

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
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            KnownDeploymentsApi knownDeployments = new KnownDeploymentsApi();
            context.registerCapability(RuntimeCapability.Builder
                    .of(CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, true, knownDeployments).build()
                    .fromBaseCapability(context.getCurrentAddressValue()));
            context.attach(KNOWN_DEPLOYMENTS_KEY, knownDeployments);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            ModelNode model = resource.getModel();
            CapabilityServiceTarget target = context.getCapabilityServiceTarget();

            final String securityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
            final String httpServerMechanismFactory = HTTP_AUTHENTICATION_FACTORY.resolveModelAttribute(context, model).asStringOrNull();
            boolean overrideDeploymentConfig = OVERRIDE_DEPLOYMENT_CONFIG.resolveModelAttribute(context, model).asBoolean();
            boolean enableJacc = ENABLE_JACC.resolveModelAttribute(context, model).asBoolean();

            String securityDomainName = context.getCurrentAddressValue();

            ServiceName applicationSecurityDomainName = APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress());

            ServiceBuilder<?> serviceBuilder = target
                    .addService(applicationSecurityDomainName)
                    .setInitialMode(Mode.LAZY);

            final Function<String, HttpAuthenticationFactory> factoryFunction;
            if (httpServerMechanismFactory != null) {
                Supplier<HttpAuthenticationFactory> httpAuthenticationFactorySupplier = serviceBuilder.requires(context.getCapabilityServiceName(REF_HTTP_AUTHENTICATION_FACTORY, HttpAuthenticationFactory.class, httpServerMechanismFactory));
                factoryFunction = (s) -> httpAuthenticationFactorySupplier.get();
            } else {
                Supplier<SecurityDomain> securityDomainSupplier = serviceBuilder.requires(context.getCapabilityServiceName(REF_SECURITY_DOMAIN, SecurityDomain.class, securityDomain));
                factoryFunction = toHttpAuthenticationFactoryFunction(securityDomainSupplier);
            }

            if (enableJacc) {
                serviceBuilder.requires(context.getCapabilityServiceName(REF_JACC_POLICY, Policy.class));
            }

            final Supplier<UnaryOperator<HttpServerAuthenticationMechanismFactory>> transformerSupplier;
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

                Supplier<SingleSignOnSessionFactory> singleSignOnSessionFactorySupplier = serviceBuilder.requires(factoryConfigurator.getServiceName());
                UnaryOperator<HttpServerAuthenticationMechanismFactory> transformer = (factory) -> new SingleSignOnServerMechanismFactory(factory, singleSignOnSessionFactorySupplier.get(), singleSignOnConfiguration);
                transformerSupplier = () -> transformer;

            } else {
                transformerSupplier = () -> null;
            }

            Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> valueConsumer = serviceBuilder.provides(applicationSecurityDomainName);
            ApplicationSecurityDomainService service = new ApplicationSecurityDomainService(overrideDeploymentConfig, enableJacc, factoryFunction, transformerSupplier, valueConsumer);
            serviceBuilder.setInstance(service);
            serviceBuilder.install();

            KnownDeploymentsApi knownDeploymentsApi = context.getAttachment(KNOWN_DEPLOYMENTS_KEY);
            knownDeploymentsApi.setApplicationSecurityDomainService(service);
        }

    }

    private static Function<String, HttpAuthenticationFactory> toHttpAuthenticationFactoryFunction(final Supplier<SecurityDomain> securityDomainSupplier) {
        final HttpServerAuthenticationMechanismFactory mechanismFactory = new FilterServerMechanismFactory(new ServerMechanismFactoryImpl(), SERVLET_MECHANISM);
        return (realmName) -> HttpAuthenticationFactory.builder().setFactory(mechanismFactory)
                .setSecurityDomain(securityDomainSupplier.get())
                .setMechanismConfigurationSelector(
                        MechanismConfigurationSelector.constantSelector(realmName == null ? MechanismConfiguration.EMPTY
                                : MechanismConfiguration.builder()
                                        .addMechanismRealm(
                                                MechanismRealmConfiguration.builder().setRealmName(realmName).build())
                                        .build()))
                .build();
    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {

        /**
         * @param addOperation
         */
        protected RemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation, APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY, APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS_CAPABILITY);
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

    private static class ApplicationSecurityDomainService implements Service, BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> {

        private final Function<String, HttpAuthenticationFactory> factoryFunction;
        private final Supplier<UnaryOperator<HttpServerAuthenticationMechanismFactory>> singleSignOnTransformerSupplier;

        private final Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> valueConsumer;

        private final boolean overrideDeploymentConfig;
        private final Set<RegistrationImpl> registrations = new HashSet<>();
        private final boolean enableJacc;

        private ApplicationSecurityDomainService(final boolean overrideDeploymentConfig, boolean enableJacc, Function<String, HttpAuthenticationFactory> factoryFunction, Supplier<UnaryOperator<HttpServerAuthenticationMechanismFactory>> singleSignOnTransformerSupplier, Consumer<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> valueConsumer) {
            this.overrideDeploymentConfig = overrideDeploymentConfig;
            this.enableJacc = enableJacc;
            this.factoryFunction = factoryFunction;
            this.singleSignOnTransformerSupplier = singleSignOnTransformerSupplier;
            this.valueConsumer = valueConsumer;
        }

        @Override
        public void start(StartContext context) throws StartException {
            valueConsumer.accept(this);
        }

        @Override
        public void stop(StopContext context) {}

        @Override
        public Registration apply(DeploymentInfo deploymentInfo, Function<String, RunAsIdentityMetaData> runAsMapper) {
            AuthenticationManager.Builder builder = AuthenticationManager.builder()
                    .setHttpAuthenticationFactory(factoryFunction.apply(getRealmName(deploymentInfo)))
                    .setOverrideDeploymentConfig(overrideDeploymentConfig)
                    .setHttpAuthenticationFactoryTransformer(singleSignOnTransformerSupplier.get())
                    .setRunAsMapper(runAsMapper);

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
