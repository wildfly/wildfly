/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.function.Function;

import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * SPI for providing local services.
 * @author Paul Ferraro
 */
public interface LocalServiceInstallerProvider extends Function<String, Iterable<ServiceInstaller>> {

}
