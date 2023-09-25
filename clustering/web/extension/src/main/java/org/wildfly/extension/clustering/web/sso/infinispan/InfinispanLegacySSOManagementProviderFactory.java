/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.infinispan;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;
import org.wildfly.clustering.web.service.sso.LegacySSOManagementProviderFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacySSOManagementProviderFactory.class)
public class InfinispanLegacySSOManagementProviderFactory implements LegacySSOManagementProviderFactory, InfinispanSSOManagementConfiguration {

    @Override
    public DistributableSSOManagementProvider createSSOManagementProvider() {
        return new InfinispanSSOManagementProvider(this);
    }

    @Override
    public String getContainerName() {
        return "web";
    }

    @Override
    public String getCacheName() {
        return null;
    }
}
