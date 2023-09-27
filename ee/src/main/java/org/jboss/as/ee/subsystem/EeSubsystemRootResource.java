/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.structure.AnnotationPropertyReplacementProcessor;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ee.structure.DescriptorPropertyReplacementProcessor;
import org.jboss.as.ee.structure.GlobalDirectoryDependencyProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the EE subsystem's root management resource.
 *
 * @author Stuart Douglas
 */
public class EeSubsystemRootResource extends SimpleResourceDefinition {

    public static final String WILDFLY_NAMING = "org.wildfly.naming";
    public static final String JBOSS_INVOCATION = "org.jboss.invocation";
    public static final String JSON_API = "jakarta.json.api";
    public static final String GLASSFISH_EL = "org.glassfish.javax.el";

    public static final SimpleAttributeDefinition EAR_SUBDEPLOYMENTS_ISOLATED =
            new SimpleAttributeDefinitionBuilder(EESubsystemModel.EAR_SUBDEPLOYMENTS_ISOLATED, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    public static final SimpleAttributeDefinition SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT =
            new SimpleAttributeDefinitionBuilder(EESubsystemModel.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    public static final SimpleAttributeDefinition JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT =
            new SimpleAttributeDefinitionBuilder(EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    public static final SimpleAttributeDefinition ANNOTATION_PROPERTY_REPLACEMENT =
            new SimpleAttributeDefinitionBuilder(EESubsystemModel.ANNOTATION_PROPERTY_REPLACEMENT, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {GlobalModulesDefinition.INSTANCE, EAR_SUBDEPLOYMENTS_ISOLATED,
            SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT, JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT, ANNOTATION_PROPERTY_REPLACEMENT};

    // Our different operation handlers manipulate the state of the subsystem's DUPs, so they need to share a ref
    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor = new DefaultEarSubDeploymentsIsolationProcessor();
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor = new GlobalModuleDependencyProcessor();
    private final GlobalDirectoryDependencyProcessor directoryDependencyProcessor = new GlobalDirectoryDependencyProcessor();
    private final DescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor = new DescriptorPropertyReplacementProcessor(Attachments.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT);
    private final DescriptorPropertyReplacementProcessor jbossDescriptorPropertyReplacementProcessor = new DescriptorPropertyReplacementProcessor(Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
    private final AnnotationPropertyReplacementProcessor annotationPropertyReplacementProcessor = new AnnotationPropertyReplacementProcessor(Attachments.ANNOTATION_PROPERTY_REPLACEMENT);

    private EeSubsystemRootResource() {
        super(EeExtension.PATH_SUBSYSTEM,
                EeExtension.getResourceDescriptionResolver(EeExtension.SUBSYSTEM_NAME),
                null,
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {
        super.registerOperations(rootResourceRegistration);
        final EeSubsystemAdd subsystemAdd = new EeSubsystemAdd(isolationProcessor, moduleDependencyProcessor,
                specDescriptorPropertyReplacementProcessor,
                jbossDescriptorPropertyReplacementProcessor,
                annotationPropertyReplacementProcessor,
                directoryDependencyProcessor
        );
        registerAddOperation(rootResourceRegistration, subsystemAdd);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        EeWriteAttributeHandler writeHandler = new EeWriteAttributeHandler(isolationProcessor, moduleDependencyProcessor,
                specDescriptorPropertyReplacementProcessor, jbossDescriptorPropertyReplacementProcessor, annotationPropertyReplacementProcessor);
        writeHandler.registerAttributes(rootResourceRegistration);
    }

    protected static EeSubsystemRootResource create(){
        return new EeSubsystemRootResource();
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(WILDFLY_NAMING),
             RuntimePackageDependency.required(JBOSS_INVOCATION),
             RuntimePackageDependency.optional(JSON_API),
             RuntimePackageDependency.optional(GLASSFISH_EL));
    }
}
