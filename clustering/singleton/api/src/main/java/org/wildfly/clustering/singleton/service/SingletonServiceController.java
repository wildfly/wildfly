/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.singleton.Singleton;

/**
 * A controller for a singleton service instance.
 * @author Paul Ferraro
 */
public interface SingletonServiceController<T> extends Singleton, ServiceController<T> {
}
