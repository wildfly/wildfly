/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacySessionManagementProviderFactory<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> {
    DistributableSessionManagementProvider<C> createSessionManagerProvider(DeploymentUnit unit, ReplicationConfig config);
}
