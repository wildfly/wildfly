/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceBuilder;

/**
 * Extends {@link ServiceBuilder} to facilitate building singleton services.
 * @author Paul Ferraro
 */
public interface SingletonServiceBuilder<T> extends ServiceBuilder<T> {

}
