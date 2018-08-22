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

package org.wildfly.extension.clustering.singleton;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.SingletonRequirement;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentParsingProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentSchema;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentXMLReader;

/**
 * Definition of the singleton deployer resource.
 * @author Paul Ferraro
 */
public class SingletonResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> implements Consumer<DeploymentProcessorTarget> {

    static final PathElement PATH = pathElement(SingletonExtension.SUBSYSTEM_NAME);

    enum Capability implements CapabilityProvider {
        @Deprecated DEFAULT_LEGACY_POLICY(SingletonDefaultRequirement.SINGLETON_POLICY),
        DEFAULT_POLICY(SingletonDefaultRequirement.POLICY),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(Requirement requirement) {
            this.capability = new RequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT("default", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_POLICY, SingletonRequirement.POLICY)),
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setCapabilityReference(reference)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public SimpleAttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static TransformationDescription buildTransformers(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        SingletonPolicyResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    SingletonResourceDefinition() {
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SingletonServiceHandler();
        new DeploymentChainContributingResourceRegistration(descriptor, handler, this).register(registration);

        new SingletonPolicyResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        JBossAllXmlParserRegisteringProcessor.Builder builder = JBossAllXmlParserRegisteringProcessor.builder();
        for (SingletonDeploymentSchema schema : SingletonDeploymentSchema.values()) {
            builder.addParser(schema.getRoot(), SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY, new SingletonDeploymentXMLReader(schema));
        }
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_SINGLETON_DEPLOYMENT, builder.build());
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_SINGLETON_DEPLOYMENT, new SingletonDeploymentParsingProcessor());
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SINGLETON_DEPLOYMENT, new SingletonDeploymentDependencyProcessor());
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_SINGLETON_DEPLOYMENT, new SingletonDeploymentProcessor());
    }
}
