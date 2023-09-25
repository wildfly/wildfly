/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.ejb3.component.stateful.cache.simple.SimpleStatefulSessionBeanCacheProviderServiceConfigurator;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are non distributed and do not have passivation-enabled.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class SimpleStatefulSessionBeanCacheProviderResourceDefinition extends StatefulSessionBeanCacheProviderResourceDefinition {

    public SimpleStatefulSessionBeanCacheProviderResourceDefinition() {
        super(EJB3SubsystemModel.SIMPLE_CACHE_PATH, UnaryOperator.identity(), SimpleStatefulSessionBeanCacheProviderServiceConfigurator::new);
    }
}
