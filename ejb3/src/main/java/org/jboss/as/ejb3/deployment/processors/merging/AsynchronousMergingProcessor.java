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
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.processors.dd.MethodResolutionUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AsyncMethodMetaData;
import org.jboss.metadata.ejb.spec.AsyncMethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import javax.ejb.Asynchronous;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class AsynchronousMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription>{

    public AsynchronousMergingProcessor() {
        super(SessionBeanComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        final RuntimeAnnotationInformation<Boolean> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Asynchronous.class);
        for(Map.Entry<String, List<Boolean>> entry : data.getClassAnnotations().entrySet()) {
            if(!entry.getValue().isEmpty()) {
                description.addAsynchronousView(entry.getKey());
            }
        }

        for(Map.Entry<Method, List<Boolean>> entry : data.getMethodAnnotations().entrySet()) {
            if(!entry.getValue().isEmpty()) {
                description.addAsynchronousMethod(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        final SessionBeanMetaData data = description.getDescriptorData();
        if(data == null) {
            return;
        }
        if(data instanceof SessionBean31MetaData) {
            final SessionBean31MetaData sessionBeanData = (SessionBean31MetaData)data;
            final AsyncMethodsMetaData asyn = sessionBeanData.getAsyncMethods();
            if(asyn != null) {
                for(AsyncMethodMetaData method : asyn) {
                    final Method m = MethodResolutionUtils.resolveMethod(method.getMethodName(), method.getMethodParams(), componentClass, deploymentReflectionIndex);
                    description.addAsynchronousMethod(MethodIdentifier.getIdentifierForMethod(m));
                }
            }
        }

    }
}
