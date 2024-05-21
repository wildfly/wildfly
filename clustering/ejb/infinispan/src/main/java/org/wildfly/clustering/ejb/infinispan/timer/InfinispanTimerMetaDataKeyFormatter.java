/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.cache.KeyFormatter;
import org.wildfly.clustering.marshalling.Formatter;
import org.wildfly.clustering.marshalling.util.UUIDSerializer;

/**
 * Serializer for timer keys.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class InfinispanTimerMetaDataKeyFormatter extends KeyFormatter<UUID, InfinispanTimerMetaDataKey<UUID>> {

    @SuppressWarnings("unchecked")
    public InfinispanTimerMetaDataKeyFormatter() {
        super((Class<InfinispanTimerMetaDataKey<UUID>>) (Class<?>) InfinispanTimerMetaDataKey.class, UUIDSerializer.INSTANCE.toFormatter(UUID.class), InfinispanTimerMetaDataKey::new);
    }
}
