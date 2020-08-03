/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.security.Policy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
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
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.security.ApplicationSecurityDomainConfig;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain, as specified in a web application,
 * to an Elytron {@link SecurityDomain}.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ApplicationSecurityDomainDefinition extends SimpleResourceDefinition {

    public static final String APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME = "org.wildfly.ejb3.application-security-domain";
    public static final String CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS = "org.wildfly.ejb3.application-security-domain.known-deployments";
    private static final String SECURITY_DOMAIN_CAPABILITY_NAME = "org.wildfly.security.security-domain";
    private static final String JACC_POLICY_CAPABILITY_NAME = "org.wildfly.security.jacc-policy";

    static final RuntimeCapability<Void> APPLICATION_SECURITY_DOMAIN_CAPABILITY = RuntimeCapability
            .Builder.of(APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME, true, ApplicationSecurityDomain.class)
            .build();

    static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SECURITY_DOMAIN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setCapabilityReference(SECURITY_DOMAIN_CAPABILITY_NAME, APPLICATION_SECURITY_DOMAIN_CAPABILITY_NAME, true)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .build();

    private static StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(EJB3SubsystemModel.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition ENABLE_JACC = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            //.setCapabilityReference(new BooleanCapabilityReferenceRecorder(EJB3SubsystemModel.ENABLE_JACC, JACC_POLICY_CAPABILITY_NAME))
            .setMinSize(1)
            .setRestartAllServices()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { SECURITY_DOMAIN, ENABLE_JACC };

    static final ApplicationSecurityDomainDefinition INSTANCE = new ApplicationSecurityDomainDefinition();

    private static final Set<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private static final AttachmentKey<KnownDeploymentsApi> KNOWN_DEPLOYMENTS_KEY = AttachmentKey.create(KnownDeploymentsApi.class);

    private ApplicationSecurityDomainDefinition() {
        this(new Parameters(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN), EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN))
                .setCapabilities(APPLICATION_SECURITY_DOMAIN_CAPABILITY)
                .addAccessConstraints(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(EJB3Extension.SUBSYSTEM_NAME, EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, false, false, false)),
                        new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(EJB3Extension.SUBSYSTEM_NAME, EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN)))
                , new AddHandler());
    }

    private ApplicationSecurityDomainDefinition(Parameters parameters, AbstractAddStepHandler add) {
        super(parameters.setAddHandler(add).setRemoveHandler(new RemoveHandler(add)));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        knownApplicationSecurityDomains.clear();
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            if (attribute != ENABLE_JACC) {
                resourceRegistration.registerReadWriteAttribute(attribute, null, handler);
            }
        }
        resourceRegistration.registerReadWriteAttribute(ENABLE_JACC, null, new ReloadRequiredWriteAttributeHandler(ENABLE_JACC) {
            @Override
            protected void recordCapabilitiesAndRequirements(OperationContext context, AttributeDefinition attributeDefinition, ModelNode newValue, ModelNode oldValue) {
                super.recordCapabilitiesAndRequirements(context, attributeDefinition, newValue, oldValue);

                // We can't process expressions
                if (newValue.getType() != ModelType.EXPRESSION) {
                    final String dependentName = APPLICATION_SECURITY_DOMAIN_CAPABILITY.getDynamicName(context.getCurrentAddressValue());
                    if (Boolean.parseBoolean(newValue.asString())) {
                        context.registerAdditionalCapabilityRequirement(JACC_POLICY_CAPABILITY_NAME, dependentName, ENABLE_JACC.getName());
                    } else {
                        context.deregisterCapabilityRequirement(JACC_POLICY_CAPABILITY_NAME, dependentName, ENABLE_JACC.getName());
                    }
                }
            }
        });
        if (resourceRegistration.getProcessType().isServer()) {
            resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
        }
    }

    private static class AddHandler extends AbstractAddStepHandler {

        private AddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            KnownDeploymentsApi knownDeployments = new KnownDeploymentsApi();
            context.registerCapability(RuntimeCapability.Builder
                    .of(CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS, true, knownDeployments).build()
                    .fromBaseCapability(context.getCurrentAddressValue()));
            context.attach(KNOWN_DEPLOYMENTS_KEY, knownDeployments);

            ModelNode enableJacc = resource.getModel().get(ENABLE_JACC.getName());
            if (enableJacc.getType() != ModelType.EXPRESSION) {
                if (Boolean.parseBoolean(enableJacc.asString())) {
                    context.registerAdditionalCapabilityRequirement(JACC_POLICY_CAPABILITY_NAME,
                            APPLICATION_SECURITY_DOMAIN_CAPABILITY.getDynamicName(context.getCurrentAddressValue()), ENABLE_JACC.getName());
                }
            }
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            ModelNode model = resource.getModel();
            boolean enableJacc = false;

            if (model.hasDefined(ENABLE_JACC.getName())) {
                enableJacc = ENABLE_JACC.resolveModelAttribute(context, model).asBoolean();
            }

            knownApplicationSecurityDomains.add(new ApplicationSecurityDomainConfig(context.getCurrentAddressValue(), enableJacc));
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String securityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();
            boolean enableJacc = ENABLE_JACC.resolveModelAttribute(context, model).asBoolean();

            RuntimeCapability<?> runtimeCapability = APPLICATION_SECURITY_DOMAIN_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName serviceName = runtimeCapability.getCapabilityServiceName(ApplicationSecurityDomain.class);
            ServiceName securityDomainServiceName = serviceName.append(Constants.SECURITY_DOMAIN);

            CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addCapability(APPLICATION_SECURITY_DOMAIN_CAPABILITY)
                    .setInitialMode(Mode.LAZY);

            Supplier<SecurityDomain> securityDomainSupplier = serviceBuilder.requires(context.getCapabilityServiceName(
                    SECURITY_DOMAIN_CAPABILITY_NAME, securityDomain, SecurityDomain.class));

            Consumer<ApplicationSecurityDomainService.ApplicationSecurityDomain> applicationSecurityDomainConsumer = serviceBuilder.provides(serviceName);
            Consumer<SecurityDomain> securityDomainConsumer = serviceBuilder.provides(securityDomainServiceName);

            ApplicationSecurityDomainService service = new ApplicationSecurityDomainService(enableJacc, securityDomainSupplier, applicationSecurityDomainConsumer, securityDomainConsumer);
            serviceBuilder.setInstance(service);

            if (model.hasDefined(ENABLE_JACC.getName())) {
                if (ENABLE_JACC.resolveModelAttribute(context, model).asBoolean()) {
                    serviceBuilder.requires(context.getCapabilityServiceName(JACC_POLICY_CAPABILITY_NAME, Policy.class));
                }
            }

            serviceBuilder.install();

            KnownDeploymentsApi knownDeploymentsApi = context.getAttachment(KNOWN_DEPLOYMENTS_KEY);
            knownDeploymentsApi.setApplicationSecurityDomainService(service);
        }
    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {

        protected RemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation);
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            HashSet<ApplicationSecurityDomainConfig> applicationSecurityDomainConfigs;
            synchronized (knownApplicationSecurityDomains) {
                applicationSecurityDomainConfigs = new HashSet<>(knownApplicationSecurityDomains);
            }
            for (ApplicationSecurityDomainConfig domain : applicationSecurityDomainConfigs) {
                if (domain.isSameDomain(context.getCurrentAddressValue())) {
                    knownApplicationSecurityDomains.remove(domain);
                }
            }

        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            context.deregisterCapabilityRequirement(JACC_POLICY_CAPABILITY_NAME, APPLICATION_SECURITY_DOMAIN_CAPABILITY.getDynamicName(context.getCurrentAddressValue()), ENABLE_JACC.getName());
        }

        @Override
        protected ServiceName serviceName(String name) {
            return APPLICATION_SECURITY_DOMAIN_CAPABILITY.getCapabilityServiceName(ApplicationSecurityDomain.class, name);
        }
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

    private static class KnownDeploymentsApi {

        private volatile ApplicationSecurityDomainService service;

        List<String> getKnownDeployments() {
            return service != null ? Arrays.asList(service.getDeployments()) : Collections.emptyList();

        }

        void setApplicationSecurityDomainService(final ApplicationSecurityDomainService service) {
            this.service = service;
        }
    }

    Function<String, ApplicationSecurityDomainConfig> getKnownSecurityDomainFunction() {
        return name -> {
            synchronized (knownApplicationSecurityDomains) {
                for (ApplicationSecurityDomainConfig applicationSecurityDomainConfig : knownApplicationSecurityDomains) {
                    if (applicationSecurityDomainConfig.isSameDomain(name)) {
                        return applicationSecurityDomainConfig;
                    }
                }
            }
            return null;
        };
    }
}
