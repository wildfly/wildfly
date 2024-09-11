/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.buildcompatible.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.version.Stability;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.IntVersion;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

@MetaInfServices(Extension.class)
public class BCEExtension extends SubsystemExtension<BCEExtension.BCESubsystemSchema> {

    public BCEExtension() {
        super(SubsystemConfiguration.of(BCESubsystemRegistrar.NAME, BCESubsystemModel.CURRENT,
                BCESubsystemRegistrar::new), SubsystemPersistence.of(BCESubsystemSchema.CURRENT));
    }

    /**
     * Model for the 'bce' subsystem.
     */
    public enum BCESubsystemModel implements SubsystemModel {
        VERSION_1_0_0(1, 0, 0),
        ;

        static final BCESubsystemModel CURRENT = VERSION_1_0_0;

        private final ModelVersion version;

        BCESubsystemModel(int major, int minor, int micro) {
            this.version = ModelVersion.create(major, minor, micro);
        }

        @Override
        public ModelVersion getVersion() {
            return this.version;
        }
    }

    /**
     * Schema for the 'bce' subsystem.
     */
    public enum BCESubsystemSchema implements PersistentSubsystemSchema<BCESubsystemSchema> {

        VERSION_1_0(1, 0, Stability.DEFAULT),
        ;

        static final BCESubsystemSchema CURRENT = VERSION_1_0;

        private final VersionedNamespace<IntVersion, BCESubsystemSchema> namespace;

        BCESubsystemSchema(int major, int minor, Stability stability) {
            this.namespace = SubsystemSchema.createSubsystemURN(BCESubsystemRegistrar.NAME, stability, new IntVersion(major, minor));
        }

        @Override
        public VersionedNamespace<IntVersion, BCESubsystemSchema> getNamespace() {
            return this.namespace;
        }

        @Override
        public Stability getStability() {
            return Stability.DEFAULT;
        }

        @Override
        public PersistentResourceXMLDescription getXMLDescription() {
            PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
            return factory.builder(BCESubsystemRegistrar.PATH).build();
        }
    }

    /**
     * Registrar for the 'bce' subsystem root resource.
     */
    private static final class BCESubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

        static final String NAME = "bce";
        static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(NAME);
        static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(NAME, BCESubsystemRegistrar.class);

        @Override
        public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
            ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
            ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                    .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                    .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                    .withRuntimeHandler(new ResourceOperationRuntimeHandler() {
                        @Override
                        public void addRuntime(OperationContext context, ModelNode model) {
                            if (context.isBooting()) {
                                context.addStep(new AbstractDeploymentChainStep() {
                                    @Override
                                    protected void execute(DeploymentProcessorTarget processorTarget) {
                                        processorTarget.addDeploymentProcessor(NAME, Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new BCEDeploymentUnitProcessor());

                                    }
                                }, OperationContext.Stage.RUNTIME);
                            } else {
                                context.reloadRequired();
                            }
                        }

                        @Override
                        public void removeRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
                            context.reloadRequired();
                        }
                    })
                    .build();
            ManagementResourceRegistrar.of(descriptor).register(registration);
            return registration;
        }
    }

    private static final class BCEDeploymentUnitProcessor implements DeploymentUnitProcessor {

        @Override
        public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            try {
                final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                        .getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
                weldCapability.registerBuildCompatibleExtension(RegisteredExtension.class, deploymentUnit);
            } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
