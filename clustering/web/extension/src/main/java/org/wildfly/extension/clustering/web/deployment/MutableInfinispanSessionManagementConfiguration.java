/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorProvider;

/**
 * Mutable Infinispan session management configuration.
 * @author Paul Ferraro
 */
public class MutableInfinispanSessionManagementConfiguration extends MutableSessionManagementConfiguration {

    private String containerName;
    private String cacheName;

    public MutableInfinispanSessionManagementConfiguration(UnaryOperator<String> replacer, Consumer<String> accumulator) {
        super(replacer, accumulator, new PrimaryOwnerRouteLocatorProvider());
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
