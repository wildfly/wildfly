/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.Service;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Implemented by the instrumented singleton service.
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public interface SingletonService extends Singleton, Service {

}
