/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Server;

/**
 * @author Paul Ferraro
 */
public enum UndertowUnaryRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    SERVER(Capabilities.CAPABILITY_SERVER, Server.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

    UndertowUnaryRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
