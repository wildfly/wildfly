/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;

/**
 * Configuration of an Infinispan routing provider
 * @author Paul Ferraro
 */
public interface InfinispanRoutingConfiguration extends InfinispanCacheConfiguration, Consumer<ConfigurationBuilder> {
}
