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

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.Container;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Serialization proxy for passivation capable contextuals that are not serializable.
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableContextualProxy<C extends Contextual<I> & PassivationCapable, I> implements Serializable {
    private static final long serialVersionUID = 4906800089125597758L;

    private final String contextId;
    private final String id;

    PassivationCapableSerializableContextualProxy(String contextId, C contextual) {
        this.contextId = contextId;
        this.id = contextual.getId();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object readResolve() {
        C contextual = Container.instance(this.contextId).services().get(ContextualStore.class).getContextual(this.id);
        return (contextual instanceof Bean) ? new PassivationCapableSerializableBean(this.contextId, Reflections.<Bean<I>>cast(contextual)) : new PassivationCapableSerializableContextual(this.contextId, contextual);
    }
}
