/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;

/**
 * Offers semantics similar to a {@link java.rmi.MarshalledObject#get()}, but supports an independent marshalling context.s
 * @author Paul Ferraro
 * @param <T> value type
 * @param <C> marshalling context type
 */
public interface MarshalledValue<T, C> {
    T get(C context) throws IOException;
}
