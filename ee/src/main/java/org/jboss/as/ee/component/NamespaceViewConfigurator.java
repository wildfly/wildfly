/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * @author John Bailey
 */
public class NamespaceViewConfigurator implements ViewConfigurator {
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
        configuration.addViewInterceptor(componentConfiguration.getNamespaceContextInterceptorFactory(), InterceptorOrder.View.JNDI_NAMESPACE_INTERCEPTOR);
    }
}
