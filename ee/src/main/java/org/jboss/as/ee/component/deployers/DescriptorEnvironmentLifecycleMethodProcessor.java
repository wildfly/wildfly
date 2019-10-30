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
package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;


/**
 * Deployment descriptor that resolves interceptor methods defined in ejb-jar.xml that could not be resolved at
 * DD parse time.
 *
 * @author Stuart Douglas
 */
public class DescriptorEnvironmentLifecycleMethodProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);

        if (environment != null) {
            handleMethods(environment, eeModuleDescription, null);
        }
    }


    public static void handleMethods(DeploymentDescriptorEnvironment env, EEModuleDescription eeModuleDescription, String defaultClassName) throws DeploymentUnitProcessingException {

        final RemoteEnvironment environment = env.getEnvironment();

        // post-construct(s) of the interceptor configured (if any) in the deployment descriptor
        LifecycleCallbacksMetaData postConstructs = environment.getPostConstructs();
        if (postConstructs != null) {
            for (LifecycleCallbackMetaData postConstruct : postConstructs) {
                String className = postConstruct.getClassName();
                if (className == null || className.isEmpty()) {
                    if (defaultClassName == null) {
                        continue;
                    } else {
                        className = defaultClassName;
                    }
                }
                final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                String methodName = postConstruct.getMethodName();
                MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                builder.setPostConstruct(methodIdentifier);
                eeModuleDescription.addInterceptorMethodOverride(className, builder.build());
            }
        }

        // pre-destroy(s) of the interceptor configured (if any) in the deployment descriptor
        LifecycleCallbacksMetaData preDestroys = environment.getPreDestroys();
        if (preDestroys != null) {
            for (LifecycleCallbackMetaData preDestroy : preDestroys) {
                String className = preDestroy.getClassName();
                if (className == null || className.isEmpty()) {
                    if (defaultClassName == null) {
                        continue;
                    } else {
                        className = defaultClassName;
                    }
                }
                final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                String methodName = preDestroy.getMethodName();
                MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                builder.setPreDestroy(methodIdentifier);
                eeModuleDescription.addInterceptorMethodOverride(className, builder.build());
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
