/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.marshalling.spi.Marshaller;

/**
 * @author Paul Ferraro
 */
public interface TimerMetaDataConfiguration<V> extends InfinispanConfiguration {

    Marshaller<Object, V> getMarshaller();
    boolean isPersistent();
}
