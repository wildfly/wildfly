/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
