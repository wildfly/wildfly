/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.util.function.Supplier;

import org.wildfly.clustering.web.cache.Contextual;

/**
 * @author Paul Ferraro
 */
public interface SessionMetaDataEntry<C> extends Contextual<C> {

    SessionCreationMetaDataEntry<C> getCreationMetaDataEntry();

    SessionAccessMetaDataEntry getAccessMetaDataEntry();

    @Override
    default C getContext(Supplier<C> factory) {
        return this.getCreationMetaDataEntry().getContext(factory);
    }
}
