/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.compat;

/**
 * Compatibility {@link SingletonServiceTargetFactory} extension that also implements legacy singleton service mechanisms.
 * @author Paul Ferraro
 */
@Deprecated
public interface SingletonServiceTargetFactory extends org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory, SingletonServiceBuilderFactory, SingletonServiceConfiguratorFactory {
}
