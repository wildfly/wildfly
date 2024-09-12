/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.wildfly.clustering.web.container.SessionManagementProvider;

/**
 * Returns the appropriate {@link SessionManagementProvider} for the given deployment unit.
 * @author Paul Ferraro
 */
public interface SessionManagementProviderFactory {
    /**
     * Returns the appropriate {@link SessionManagementProvider} for the specified deployment unit,
     * generated from the specified {@link ReplicationConfig} if necessary.
     * @param context a deployment phase context
     * @param config a legacy {@link ReplicationConfig}
     * @return a session management provider
     */
    SessionManagementProvider createSessionManagementProvider(DeploymentPhaseContext context, ReplicationConfig config);
}
