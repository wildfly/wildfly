/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.subsystem.AccessLogResourceDefinition;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * Registers {@link EjbAccessLogInterceptor} on every EJB business view at
 * {@link InterceptorOrder.View#ACCESS_LOG_INTERCEPTOR} (0x280), provided the access-log feature is
 * enabled at the server's current stability level (see {@link #isEnabled(DeploymentUnit)}).
 *
 * <p>When the feature is enabled, this configurator is added regardless of whether the
 * {@code service=access-log} resource has been added. At invocation time the interceptor reads the
 * {@link AccessLogHolder} and returns immediately if access logging is not configured.
 *
 * <p>The timeout-view registration (for timer-driven invocations) is handled separately in
 * {@link EJBComponentDescription} because the timeout view is built by a different code path.
 */
public final class AccessLogViewConfigurator implements ViewConfigurator {

    public static final AccessLogViewConfigurator INSTANCE = new AccessLogViewConfigurator();

    private AccessLogViewConfigurator() {
    }

    /**
     * Returns {@code true} if the EJB access-log feature is enabled at the stability level of the given deployment.
     * This is the same condition under which the {@link AccessLogHolder} service is installed.
     *
     * @param deploymentUnit the deployment unit being processed
     * @return {@code true} if the access-log interceptor and its holder dependency should be configured
     */
    static boolean isEnabled(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getStability().enables(AccessLogResourceDefinition.STABILITY);
    }

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration,
                          final ViewDescription description, final ViewConfiguration viewConfiguration)
            throws DeploymentUnitProcessingException {
        if (isEnabled(context.getDeploymentUnit())) {
            viewConfiguration.addViewInterceptor(EjbAccessLogInterceptor.FACTORY, InterceptorOrder.View.ACCESS_LOG_INTERCEPTOR);
        }
    }
}
