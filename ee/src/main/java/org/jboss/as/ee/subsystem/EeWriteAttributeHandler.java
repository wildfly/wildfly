/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.structure.AnnotationPropertyReplacementProcessor;
import org.jboss.as.ee.structure.DescriptorPropertyReplacementProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.dmr.ModelNode;

/**
 * Handles the "write-attribute" operation for the EE subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EeWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor;
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor;
    private final DescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor;
    private final DescriptorPropertyReplacementProcessor jbossDescriptorPropertyReplacementProcessor;
    private final AnnotationPropertyReplacementProcessor annotationPropertyReplacementProcessor;

    public EeWriteAttributeHandler(final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor,
                                   final GlobalModuleDependencyProcessor moduleDependencyProcessor,
                                   final DescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor,
                                   final DescriptorPropertyReplacementProcessor jbossDescriptorPropertyReplacementProcessor,
                                   final AnnotationPropertyReplacementProcessor annotationPropertyReplacementProcessor) {
        super(EeSubsystemRootResource.ATTRIBUTES);
        this.isolationProcessor = isolationProcessor;
        this.moduleDependencyProcessor = moduleDependencyProcessor;
        this.specDescriptorPropertyReplacementProcessor = specDescriptorPropertyReplacementProcessor;
        this.jbossDescriptorPropertyReplacementProcessor = jbossDescriptorPropertyReplacementProcessor;
        this.annotationPropertyReplacementProcessor = annotationPropertyReplacementProcessor;
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition ad : EeSubsystemRootResource.ATTRIBUTES) {
            registry.registerReadWriteAttribute(ad, null, this);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {

        applyUpdateToDeploymentUnitProcessor(context, newValue, attributeName);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        applyUpdateToDeploymentUnitProcessor(context, valueToRestore, attributeName);
    }

    private void applyUpdateToDeploymentUnitProcessor(final OperationContext context, ModelNode newValue, String attributeName) throws OperationFailedException {
        if (GlobalModulesDefinition.INSTANCE.getName().equals(attributeName)) {
            moduleDependencyProcessor.setGlobalModules(GlobalModulesDefinition.createModuleList(context, newValue));
        } else if (EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.getName().equals(attributeName)) {
            boolean isolate = newValue.asBoolean();
            isolationProcessor.setEarSubDeploymentsIsolated(isolate);
        } else if (EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getName().equals(attributeName)) {
            boolean enabled = newValue.asBoolean();
            specDescriptorPropertyReplacementProcessor.setDescriptorPropertyReplacement(enabled);
        } else if (EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.getName().equals(attributeName)) {
            boolean enabled = newValue.asBoolean();
            jbossDescriptorPropertyReplacementProcessor.setDescriptorPropertyReplacement(enabled);
        } else if(EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.getName().equals(attributeName)){
            boolean enabled = newValue.asBoolean();
            annotationPropertyReplacementProcessor.setDescriptorPropertyReplacement(enabled);
        }
    }
}
