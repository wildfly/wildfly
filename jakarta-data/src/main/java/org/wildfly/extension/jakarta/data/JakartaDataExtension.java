/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.jakarta.data;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.jakarta.data._private.JakartaDataLogger.ROOT_LOGGER;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.version.Stability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.IntVersion;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;


/**
 * WildFly extension that provides Jakarta MVC support based on Eclipse Krazo.
 *
 * @author <a href="mailto:brian.stansberry@redhat.com">Brian Stansberry</a>
 */
@MetaInfServices(Extension.class)
public class JakartaDataExtension extends SubsystemExtension<JakartaDataExtension.JakartaDataSubsystemSchema> {

    /**
     * The name of our subsystem within the model.
     */
    static final String SUBSYSTEM_NAME = "jakarta-data";
    private static final Stability FEATURE_STABILITY = Stability.PREVIEW;

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    public JakartaDataExtension() {
        super(SubsystemConfiguration.of(SUBSYSTEM_NAME, JakartaDataSubsystemModel.CURRENT, JakartaDataSubsystemRegistrar::new),
                SubsystemPersistence.of(JakartaDataSubsystemSchema.CURRENT));
    }

    @Override
    public Stability getStability() {
        return FEATURE_STABILITY;
    }

    /**
     * Model for the 'jakarta-data' subsystem.
     */
    public enum JakartaDataSubsystemModel implements SubsystemModel {
        VERSION_1_0_0(1, 0, 0),
        ;

        static final JakartaDataSubsystemModel CURRENT = VERSION_1_0_0;

        private final ModelVersion version;

        JakartaDataSubsystemModel(int major, int minor, int micro) {
            this.version = ModelVersion.create(major, minor, micro);
        }

        @Override
        public ModelVersion getVersion() {
            return this.version;
        }
    }

    /**
     * Schema for the 'jakarta-data' subsystem.
     */
    public enum JakartaDataSubsystemSchema implements PersistentSubsystemSchema<JakartaDataSubsystemSchema> {

        VERSION_1_0_PREVIEW(1, 0, FEATURE_STABILITY),
        ;

        static final JakartaDataSubsystemSchema CURRENT = VERSION_1_0_PREVIEW;

        private final VersionedNamespace<IntVersion, JakartaDataSubsystemSchema> namespace;

        JakartaDataSubsystemSchema(int major, int minor, Stability stability) {
            this.namespace = SubsystemSchema.createSubsystemURN(SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
        }

        @Override
        public VersionedNamespace<IntVersion, JakartaDataSubsystemSchema> getNamespace() {
            return this.namespace;
        }

        @Override
        public Stability getStability() {
            return this.getNamespace().getStability();
        }

        @Override
        public PersistentResourceXMLDescription getXMLDescription() {
            PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
            return factory.builder(SUBSYSTEM_PATH).build();
        }
    }

    private static final class JakartaDataSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

        private static final RuntimeCapability<Void> JAKARTA_DATA_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jakarta.data")
                .addRequirements("org.wildfly.jpa")
                .build();
        static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, JakartaDataSubsystemRegistrar.class);

        static final String JAKARTA_DATA_API = "jakarta.data.api";

        @Override
        public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext managementResourceRegistrationContext) {
            ResourceDefinition definition = ResourceDefinition.builder(ResourceRegistration.of(SUBSYSTEM_PATH), RESOLVER).build();
            ManagementResourceRegistration registration = parent.registerSubsystemModel(definition);
            ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                    .addCapability(JAKARTA_DATA_CAPABILITY)
                    .withDeploymentChainContributor(JakartaDataSubsystemRegistrar::registerDeploymentUnitProcessors)
                    .build();
            ManagementResourceRegistrar.of(descriptor).register(registration);
            registration.registerAdditionalRuntimePackages(
                    RuntimePackageDependency.required(JAKARTA_DATA_API)
            );
            return registration;
        }

        private static void registerDeploymentUnitProcessors(DeploymentProcessorTarget processorTarget) {
            processorTarget.addDeploymentProcessor(JakartaDataExtension.SUBSYSTEM_NAME,
                    Phase.DEPENDENCIES,
                    Phase.DEPENDENCIES_JPA + 1, // TODO https://issues.redhat.com/browse/WFLY-21271
                    new DeploymentUnitProcessor() {
                        @Override
                        public void deploy(DeploymentPhaseContext phaseContext) {

                            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
                            final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
                            final ModuleLoader moduleLoader = Module.getBootModuleLoader();

                            // all applications get the jakarta.persistence module added to their deplyoment by default
                            addDependencies(moduleSpecification, moduleLoader, deploymentUnit, JAKARTA_DATA_API);
                        }
                    });
            processorTarget.addDeploymentProcessor(JakartaDataExtension.SUBSYSTEM_NAME,
                    Phase.POST_MODULE,
                    Phase.POST_MODULE_JMS_CDI_EXTENSIONS + 1, // TODO https://issues.redhat.com/browse/WFLY-21271
                    new JakartaDataDeploymentProcessor());
        }
    }

    private static void addDependencies(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               DeploymentUnit deploymentUnit, String... moduleIdentifiers) {
        for (String moduleIdentifier : moduleIdentifiers) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setImportServices(true).build());
            ROOT_LOGGER.debugf("added %s dependency to %s", moduleIdentifier, deploymentUnit.getName());
        }
    }
}
