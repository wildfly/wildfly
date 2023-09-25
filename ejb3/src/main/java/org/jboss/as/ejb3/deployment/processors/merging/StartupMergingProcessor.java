/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import jakarta.ejb.Startup;

/**
 * Handles {@link Startup}
 * @author Stuart Douglas
 */
public class StartupMergingProcessor extends AbstractMergingProcessor<SingletonComponentDescription> {

    public StartupMergingProcessor() {
        super(SingletonComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SingletonComponentDescription description) throws DeploymentUnitProcessingException {
        EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        if (clazz != null) {
            final ClassAnnotationInformation<Startup, Object> data = clazz.getAnnotationInformation(Startup.class);
            if (data != null
                    && !data.getClassLevelAnnotations().isEmpty()) {
                description.initOnStartup();
                description.getModuleDescription().registerStartupBean();
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SingletonComponentDescription description) throws DeploymentUnitProcessingException {
        if (!description.isInitOnStartup()) {
            SessionBeanMetaData data = description.getDescriptorData();
            if (data instanceof SessionBean31MetaData) {
                SessionBean31MetaData singletonBeanMetaData = (SessionBean31MetaData) data;
                Boolean initOnStartup = singletonBeanMetaData.isInitOnStartup();
                if (initOnStartup != null && initOnStartup) {
                    description.initOnStartup();
                    description.getModuleDescription().registerStartupBean();
                }
            }
        } // else skip. This is already marked as InitOnStartup by @Startup annotation.
    }
}
