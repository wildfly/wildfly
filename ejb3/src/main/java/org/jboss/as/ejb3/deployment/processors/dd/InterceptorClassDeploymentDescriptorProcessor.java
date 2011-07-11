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
package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AroundInvokeMetaData;
import org.jboss.metadata.ejb.spec.AroundInvokesMetaData;
import org.jboss.metadata.ejb.spec.EjbJar3xMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;

import javax.interceptor.InvocationContext;

/**
 * Processor that handles the &lt;interceptor\&gt; element of an ejb-jar.xml file.
 *
 * @author Stuart Douglas
 */
public class InterceptorClassDeploymentDescriptorProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        if (ejbJarMetaData == null) {
            return;
        }
        if (!(ejbJarMetaData instanceof EjbJar3xMetaData)) {
            return;
        }

        final EjbJar3xMetaData metaData = (EjbJar3xMetaData)ejbJarMetaData;
        if(metaData.getInterceptors() == null) {
            return;
        }

        for (InterceptorMetaData interceptor : metaData.getInterceptors()) {
            String interceptorClassName = interceptor.getInterceptorClass();
            // get (or create the interceptor description)
            EEModuleClassDescription interceptorModuleClassDescription = applicationClassesDescription.getOrAddClassByName(interceptorClassName);
            // around-invoke(s) of the interceptor configured (if any) in the deployment descriptor
            AroundInvokesMetaData aroundInvokes = interceptor.getAroundInvokes();
            if (aroundInvokes != null) {
                for (AroundInvokeMetaData aroundInvoke : aroundInvokes) {
                    String methodName = aroundInvoke.getMethodName();
                    MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Object.class, methodName, new Class<?>[]{InvocationContext.class});
                    interceptorModuleClassDescription.setAroundInvokeMethod(methodIdentifier);
                }
            }

            // post-construct(s) of the interceptor configured (if any) in the deployment descriptor
            LifecycleCallbacksMetaData postConstructs = interceptor.getPostConstructs();
            if (postConstructs != null) {
                for (LifecycleCallbackMetaData postConstruct : postConstructs) {
                    String methodName = postConstruct.getMethodName();
                    MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodName, new Class<?>[]{InvocationContext.class});
                    // add it to the interceptor description
                    interceptorModuleClassDescription.setPostConstructMethod(methodIdentifier);
                }
            }

            // pre-destroy(s) of the interceptor configured (if any) in the deployment descriptor
            LifecycleCallbacksMetaData preDestroys = interceptor.getPreDestroys();
            if (preDestroys != null) {
                for (LifecycleCallbackMetaData preDestroy : preDestroys) {
                    String methodName = preDestroy.getMethodName();
                    MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodName, new Class<?>[]{InvocationContext.class});
                    // add it to the interceptor description
                    interceptorModuleClassDescription.setPreDestroyMethod(methodIdentifier);
                }
            }
        }

    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
