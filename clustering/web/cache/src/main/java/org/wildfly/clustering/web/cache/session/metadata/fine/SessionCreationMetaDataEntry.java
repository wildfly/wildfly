/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.cache.function.Remappable;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.web.cache.Contextual;

/**
 * A contextual session metadata entry.
 * @author Paul Ferraro
 * @param <C> the context type
 */
public interface SessionCreationMetaDataEntry<C> extends SessionCreationMetaData, Contextual<C>, Remappable<SessionCreationMetaDataEntry<C>, Supplier<Offset<Duration>>> {
}
