/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;

/**
 * @author Stuart Douglas
 */
public class ConcurrencyManagementMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription> {

    public ConcurrencyManagementMergingProcessor() {
        super(SessionBeanComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        ClassAnnotationInformation<ConcurrencyManagement, ConcurrencyManagementType> management = clazz.getAnnotationInformation(ConcurrencyManagement.class);
        if (management == null) {
            return;
        }
        if (!management.getClassLevelAnnotations().isEmpty()) {
            componentConfiguration.setConcurrencyManagementType(management.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getDescriptorData() == null) {
            return;
        }
        SessionBeanMetaData data = componentConfiguration.getDescriptorData();
        if (data instanceof SessionBean31MetaData) {
            SessionBean31MetaData descriptor = (SessionBean31MetaData) data;
            final ConcurrencyManagementType type = descriptor.getConcurrencyManagementType();
            if (type != null) {
                componentConfiguration.setConcurrencyManagementType(type);
            }
        }
    }
}
