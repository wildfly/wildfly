/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Interceptor;

import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
 * Holds interceptor instances of a Jakarta EE component.
 *
 * @author Martin Kouba
 */
public interface InterceptorInstances {

    Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> getInstances();

    CreationalContext<?> getCreationalContext();

}
