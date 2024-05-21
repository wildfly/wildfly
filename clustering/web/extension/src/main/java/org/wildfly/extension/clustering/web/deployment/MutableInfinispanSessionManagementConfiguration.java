/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.UnaryOperator;

import org.wildfly.clustering.server.service.BinaryServiceConfiguration;

/**
 * @author Paul Ferraro
 */
public class MutableInfinispanSessionManagementConfiguration extends MutableSessionManagementConfiguration implements BinaryServiceConfiguration {

    private String containerName;
    private String cacheName;

    public MutableInfinispanSessionManagementConfiguration(UnaryOperator<String> replacer) {
        super(replacer);
    }

    @Override
    public String getParentName() {
        return this.containerName;
    }

    @Override
    public String getChildName() {
        return this.cacheName;
    }

    public void setContainerName(String containerName) {
        this.containerName = this.apply(containerName);
    }

    public void setCacheName(String cacheName) {
        this.cacheName = this.apply(cacheName);
    }
}
