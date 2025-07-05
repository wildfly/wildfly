/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;

/**
 * Mutable HotRod session management configuration.
 * @author Paul Ferraro
 */
public class MutableHotRodSessionManagementConfiguration extends MutableSessionManagementConfiguration {

    private volatile String containerName;
    private volatile String configurationName;

    /**
     * Constructs a new HotRod session management configuration.
     * @param replacer a property replacer
     */
    public MutableHotRodSessionManagementConfiguration(UnaryOperator<String> replacer, Consumer<String> accumulator) {
        super(replacer, accumulator, new LocalRouteLocatorProvider());
    }

    @Override
    public String getParentName() {
        return this.containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public String getChildName() {
        return this.configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }
}
