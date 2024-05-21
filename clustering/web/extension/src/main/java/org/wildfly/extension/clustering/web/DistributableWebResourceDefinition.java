/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentParsingProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentSchema;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web resource.
 * @author Paul Ferraro
 */
public class DistributableWebResourceDefinition extends SubsystemResourceDefinition implements Consumer<DeploymentProcessorTarget>, ResourceServiceConfigurator {

    static final PathElement PATH = pathElement(DistributableWebExtension.SUBSYSTEM_NAME);

    static final RuntimeCapability<Void> DEFAULT_SESSION_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(DistributableSessionManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();
    static final RuntimeCapability<Void> DEFAULT_SSO_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(DistributableUserManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_SESSION_MANAGEMENT("default-session-management", ModelType.STRING, CapabilityReferenceRecorder.builder(DEFAULT_SESSION_MANAGEMENT_PROVIDER, DistributableSessionManagementProvider.SERVICE_DESCRIPTOR).build()),
        DEFAULT_SSO_MANAGEMENT("default-single-sign-on-management", ModelType.STRING, CapabilityReferenceRecorder.builder(DEFAULT_SSO_MANAGEMENT_PROVIDER, DistributableUserManagementProvider.SERVICE_DESCRIPTOR).build()),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder<?> reference) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setCapabilityReference(reference)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    DistributableWebResourceDefinition() {
        super(PATH, DistributableWebExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(DEFAULT_SESSION_MANAGEMENT_PROVIDER, DEFAULT_SSO_MANAGEMENT_PROVIDER))
                .addRequiredSingletonChildren(LocalRoutingProviderResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new DeploymentChainContributingResourceRegistrar(descriptor, handler, this).register(registration);

        new LocalRoutingProviderResourceDefinition().register(registration);
        new InfinispanRoutingProviderResourceDefinition().register(registration);

        new InfinispanSessionManagementResourceDefinition().register(registration);
        new InfinispanUserManagementResourceDefinition().register(registration);

        new HotRodSessionManagementResourceDefinition().register(registration);
        new HotRodUserManagementResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_DISTRIBUTABLE_WEB, JBossAllSchema.createDeploymentUnitProcessor(EnumSet.allOf(DistributableWebDeploymentSchema.class), DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY));
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentParsingProcessor());
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_DISTRIBUTABLE_WEB, new DistributableWebDeploymentDependencyProcessor());
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentProcessor());
    }


    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String defaultSessionManagement = Attribute.DEFAULT_SESSION_MANAGEMENT.resolveModelAttribute(context, model).asString();
        String defaultSSOManagement = Attribute.DEFAULT_SSO_MANAGEMENT.resolveModelAttribute(context, model).asString();

        return ResourceServiceInstaller.combine(
                CapabilityServiceInstaller.builder(DEFAULT_SESSION_MANAGEMENT_PROVIDER, ServiceDependency.on(DistributableSessionManagementProvider.SERVICE_DESCRIPTOR, defaultSessionManagement)).build(),
                CapabilityServiceInstaller.builder(DEFAULT_SSO_MANAGEMENT_PROVIDER, ServiceDependency.on(DistributableUserManagementProvider.SERVICE_DESCRIPTOR, defaultSSOManagement)).build());
    }
}
