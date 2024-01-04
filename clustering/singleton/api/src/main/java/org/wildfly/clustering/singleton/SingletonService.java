/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.jboss.msc.service.Service;

/**
 * Implemented by the instrumented singleton service.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.service.SingletonService}.
 */
@Deprecated(forRemoval = true)
public interface SingletonService<T> extends org.wildfly.clustering.singleton.service.SingletonService, Service<T> {

}
