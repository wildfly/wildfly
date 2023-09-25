/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.marshalling.spi.Marshaller;

/**
 * @author Paul Ferraro
 */
public interface TimerMetaDataConfiguration<V> {

    Marshaller<Object, V> getMarshaller();
    boolean isPersistent();
}
