/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.function.Function;

import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * SPI for providing services on behalf of a default cache.
 * @author Paul Ferraro
 */
public interface DefaultCacheServiceInstallerProvider extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>> {

}
