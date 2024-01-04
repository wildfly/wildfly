/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.sso;

/**
 * @author Paul Ferraro
 */
public interface LegacySSOManagementProviderFactory {
    DistributableSSOManagementProvider createSSOManagementProvider();
}
