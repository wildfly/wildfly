/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.function.BiFunction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * SPI for providing services on behalf of a default channel.
 * @author Paul Ferraro
 */
public interface DefaultChannelServiceInstallerProvider extends BiFunction<CapabilityServiceSupport, String, Iterable<ServiceInstaller>> {

}
