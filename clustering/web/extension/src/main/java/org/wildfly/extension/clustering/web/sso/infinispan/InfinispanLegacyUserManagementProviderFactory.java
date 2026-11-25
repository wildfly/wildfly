/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.sso.infinispan;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.clustering.web.service.user.LegacyDistributableUserManagementProviderFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacyDistributableUserManagementProviderFactory.class)
public class InfinispanLegacyUserManagementProviderFactory implements LegacyDistributableUserManagementProviderFactory, BinaryServiceConfiguration {

    @Override
    public DistributableUserManagementProvider createUserManagementProvider() {
        return new InfinispanUserManagementProvider(this);
    }

    @Override
    public String getParentName() {
        return "web";
    }

    @Override
    public String getChildName() {
        return null;
    }
}
