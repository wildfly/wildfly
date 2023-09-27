/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.Serializable;

import org.jboss.weld.Container;
import org.jboss.weld.serialization.BeanIdentifierIndex;
import org.jboss.weld.serialization.spi.BeanIdentifier;

/**
 * Serialization proxy for passivation capable contextuals that are not serializable.
 * @author Paul Ferraro
 */
public abstract class PassivationCapableSerializableProxy implements Serializable {
    private static final long serialVersionUID = 4906800089125597758L;

    private final String contextId;
    private final BeanIdentifier identifier;
    private final Integer beanIndex;

    PassivationCapableSerializableProxy(String contextId, BeanIdentifier identifier) {
        this.contextId = contextId;
        BeanIdentifierIndex index = Container.instance(contextId).services().get(BeanIdentifierIndex.class);
        this.beanIndex = (index != null) && index.isBuilt() ? index.getIndex(identifier) : null;
        this.identifier = (this.beanIndex == null) ? identifier : null;
    }

    String getContextId() {
        return this.contextId;
    }

    BeanIdentifier getIdentifier() {
        return (this.identifier != null) ? this.identifier : Container.instance(this.contextId).services().get(BeanIdentifierIndex.class).getIdentifier(this.beanIndex);
    }
}
