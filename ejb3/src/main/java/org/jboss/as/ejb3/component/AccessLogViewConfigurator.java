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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * Registers {@link EjbAccessLogInterceptor} on every EJB business view at
 * {@link InterceptorOrder.View#ACCESS_LOG_INTERCEPTOR} (0x280).
 *
 * <p>This configurator is added unconditionally in {@link EJBViewDescription} so the interceptor
 * is present whether or not the {@code service=access-log} resource is deployed. At invocation
 * time the interceptor checks {@code AccessLogResourceDefinition.LIVE_SERVICE} and returns
 * immediately if access logging is not configured.
 *
 * <p>The timeout-view registration (for timer-driven invocations) is handled separately in
 * {@link EJBComponentDescription} because the timeout view is built by a different code path.
 */
public final class AccessLogViewConfigurator implements ViewConfigurator {

    public static final AccessLogViewConfigurator INSTANCE = new AccessLogViewConfigurator();

    private AccessLogViewConfigurator() {
    }

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration,
                          final ViewDescription description, final ViewConfiguration viewConfiguration)
            throws DeploymentUnitProcessingException {
        viewConfiguration.addViewInterceptor(EjbAccessLogInterceptor.FACTORY, InterceptorOrder.View.ACCESS_LOG_INTERCEPTOR);
    }
}
