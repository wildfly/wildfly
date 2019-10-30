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
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;

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
