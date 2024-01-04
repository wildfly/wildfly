/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import java.util.function.Function;

import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.marshalling.spi.SimpleFormatter;

/**
 * Base {@link org.wildfly.clustering.marshalling.spi.Formatter} for cache keys containing session identifiers.
 * @author Paul Ferraro
 */
public class SessionKeyFormatter<K extends GroupedKey<String>> extends SimpleFormatter<K> {

    protected SessionKeyFormatter(Class<K> targetClass, Function<String, K> resolver) {
        super(targetClass, resolver, GroupedKey::getId);
    }
}
