/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.ejb.Startup;

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
            if (data != null) {
                if (!data.getClassLevelAnnotations().isEmpty()) {
                    description.initOnStartup();
                    description.getModuleDescription().registerStartupBean();
                }
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
