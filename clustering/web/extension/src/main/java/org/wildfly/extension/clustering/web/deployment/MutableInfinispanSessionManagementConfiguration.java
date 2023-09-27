/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.UnaryOperator;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;

/**
 * @author Paul Ferraro
 */
public class MutableInfinispanSessionManagementConfiguration extends MutableSessionManagementConfiguration implements InfinispanSessionManagementConfiguration<DeploymentUnit> {

    private String containerName;
    private String cacheName;

    public MutableInfinispanSessionManagementConfiguration(UnaryOperator<String> replacer) {
        super(replacer);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    public void setContainerName(String containerName) {
        this.containerName = this.apply(containerName);
    }

    public void setCacheName(String cacheName) {
        this.cacheName = this.apply(cacheName);
    }
}
