/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.web.jboss.ReplicationConfig;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyDistributableSessionManagementProviderFactory {
    DistributableSessionManagementProvider createSessionManagerProvider(DeploymentUnit unit, ReplicationConfig config);
}
