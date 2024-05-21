/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.function.BiFunction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * SPI for providing services on behalf of a cache.
 * @author Paul Ferraro
 */
public interface ClusteredCacheServiceInstallerProvider extends BiFunction<CapabilityServiceSupport, BinaryServiceConfiguration, Iterable<ServiceInstaller>> {

}
