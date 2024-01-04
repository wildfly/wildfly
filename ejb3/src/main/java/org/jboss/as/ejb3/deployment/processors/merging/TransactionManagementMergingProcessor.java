/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;

/**
 * @author Stuart Douglas
 */
public class TransactionManagementMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public TransactionManagementMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        ClassAnnotationInformation<TransactionManagement, TransactionManagementType> management = clazz.getAnnotationInformation(TransactionManagement.class);
        if (management == null) {
            return;
        }
        if (!management.getClassLevelAnnotations().isEmpty()) {
            componentConfiguration.setTransactionManagementType(management.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final EnterpriseBeanMetaData beanMetaData = componentConfiguration.getDescriptorData();
        if(componentConfiguration.isEntity() || beanMetaData == null) {
            return;
        }
        final TransactionManagementType type = componentConfiguration.getDescriptorData().getTransactionType();
        if(type != null) {
            componentConfiguration.setTransactionManagementType(type);
        }
    }
}
