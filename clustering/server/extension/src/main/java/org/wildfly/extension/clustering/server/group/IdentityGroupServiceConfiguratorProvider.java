/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.extension.clustering.server.IdentityGroupRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(org.wildfly.clustering.server.service.IdentityGroupServiceConfiguratorProvider.class)
public class IdentityGroupServiceConfiguratorProvider extends IdentityGroupRequirementServiceConfiguratorProvider {

    public IdentityGroupServiceConfiguratorProvider() {
        super(ClusteringRequirement.GROUP, alias -> JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "group", alias));
    }
}
