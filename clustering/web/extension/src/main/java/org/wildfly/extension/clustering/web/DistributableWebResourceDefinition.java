/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.web.service.WebDefaultProviderRequirement;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentParsingProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentSchema;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentXMLReader;

/**
 * Definition of the /subsystem=distributable-web resource.
 * @author Paul Ferraro
 */
public class DistributableWebResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> implements Consumer<DeploymentProcessorTarget> {

    static final PathElement PATH = pathElement(DistributableWebExtension.SUBSYSTEM_NAME);

    enum Capability implements CapabilityProvider {
        DEFAULT_SESSION_MANAGEMENT_PROVIDER(WebDefaultProviderRequirement.SESSION_MANAGEMENT_PROVIDER),
        DEFAULT_SSO_MANAGEMENT_PROVIDER(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER),
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
        DEFAULT_SESSION_MANAGEMENT("default-session-management", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_SESSION_MANAGEMENT_PROVIDER, WebProviderRequirement.SESSION_MANAGEMENT_PROVIDER)),
        DEFAULT_SSO_MANAGEMENT("default-single-sign-on-management", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_SSO_MANAGEMENT_PROVIDER, WebProviderRequirement.SSO_MANAGEMENT_PROVIDER)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
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
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(LocalRoutingProviderResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new DistributableWebResourceServiceHandler();
        new DeploymentChainContributingResourceRegistration(descriptor, handler, this).register(registration);

        new LocalRoutingProviderResourceDefinition().register(registration);
        new InfinispanRoutingProviderResourceDefinition().register(registration);

        new InfinispanSessionManagementResourceDefinition().register(registration);
        new InfinispanSSOManagementResourceDefinition().register(registration);

        new HotRodSessionManagementResourceDefinition().register(registration);
        new HotRodSSOManagementResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        JBossAllXmlParserRegisteringProcessor.Builder builder = JBossAllXmlParserRegisteringProcessor.builder();
        for (DistributableWebDeploymentSchema schema : EnumSet.allOf(DistributableWebDeploymentSchema.class)) {
            builder.addParser(schema.getRoot(), DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY, new DistributableWebDeploymentXMLReader(schema));
        }
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_DISTRIBUTABLE_WEB, builder.build());
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentParsingProcessor());
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_DISTRIBUTABLE_WEB, new DistributableWebDeploymentDependencyProcessor());
        target.addDeploymentProcessor(DistributableWebExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentProcessor());
    }
}
