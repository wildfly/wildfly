/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.service;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.DefaultableBinaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableBinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public enum InfinispanCacheRequirement implements DefaultableBinaryRequirement, DefaultableBinaryServiceNameFactoryProvider {

    CACHE("org.wildfly.clustering.infinispan.cache", InfinispanDefaultCacheRequirement.CACHE),
    CONFIGURATION("org.wildfly.clustering.infinispan.cache-configuration", InfinispanDefaultCacheRequirement.CONFIGURATION),
    ;
    private final String name;
    private final BinaryServiceNameFactory factory = new BinaryRequirementServiceNameFactory(this);
    private final InfinispanDefaultCacheRequirement defaultRequirement;

    InfinispanCacheRequirement(String name, InfinispanDefaultCacheRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UnaryRequirement getDefaultRequirement() {
        return this.defaultRequirement;
    }

    @Override
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public UnaryServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}
