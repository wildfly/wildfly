/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;

/**
 * @author Paul Ferraro
 */
public enum UndertowBinaryRequirement implements BinaryRequirement, BinaryServiceNameFactoryProvider {
    HOST(Capabilities.CAPABILITY_HOST, Host.class),
    ;
    private final String name;
    private final Class<?> type;
    private final BinaryServiceNameFactory factory = new BinaryRequirementServiceNameFactory(this);

    UndertowBinaryRequirement(String name, Class<?> type) {
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
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
