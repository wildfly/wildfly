/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.cache.KeySerializer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.util.UUIDSerializer;

/**
 * Serializer for timer keys.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class InfinispanTimerMetaDataKeyFormatter extends BinaryFormatter<InfinispanTimerMetaDataKey<UUID>> {

    @SuppressWarnings("unchecked")
    public InfinispanTimerMetaDataKeyFormatter() {
        super((Class<InfinispanTimerMetaDataKey<UUID>>) (Class<?>) InfinispanTimerMetaDataKey.class, new KeySerializer<>(UUIDSerializer.INSTANCE, InfinispanTimerMetaDataKey::new));
    }
}
