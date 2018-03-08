/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.infinispan.deployment.ClusteringDependencyProcessor;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.LocalGroupBuilderProvider;

/**
 * The root resource of the Infinispan subsystem.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanSubsystemResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> implements Consumer<DeploymentProcessorTarget> {

    static final PathElement PATH = pathElement(InfinispanExtension.SUBSYSTEM_NAME);

    static final Map<ClusteringRequirement, org.jboss.as.clustering.controller.Capability> LOCAL_CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        for (ClusteringRequirement requirement : EnumSet.allOf(ClusteringRequirement.class)) {
            LOCAL_CLUSTERING_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement) {
                @Override
                public RuntimeCapability<Void> resolve(PathAddress address) {
                    return this.getDefinition().fromBaseCapability(LocalGroupBuilderProvider.LOCAL);
                }
            });
        }
    }

    static final Map<ClusteringRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        for (ClusteringRequirement requirement : EnumSet.allOf(ClusteringRequirement.class)) {
            CLUSTERING_CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement(), builder -> builder.setAllowMultipleRegistrations(true)));
        }
    }

    static TransformationDescription buildTransformation(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        CacheContainerResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    InfinispanSubsystemResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(LOCAL_CLUSTERING_CAPABILITIES.values())
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                ;
        ResourceServiceHandler handler = new InfinispanSubsystemServiceHandler();
        new DeploymentChainContributingResourceRegistration(descriptor, handler, this).register(registration);

        new CacheContainerResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(InfinispanExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_CLUSTERING, new ClusteringDependencyProcessor());
    }
}
