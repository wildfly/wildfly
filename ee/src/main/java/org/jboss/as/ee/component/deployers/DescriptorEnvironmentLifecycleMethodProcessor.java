/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}
