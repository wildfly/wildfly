/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import org.jboss.as.clustering.infinispan.invoker.Creator;
import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.jboss.as.clustering.infinispan.invoker.Locator;
import org.jboss.as.clustering.infinispan.invoker.Remover;

/**
 * A factory for creating a {@link BeanGroup}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public interface BeanGroupFactory<G, I, T> extends Creator<G, BeanGroupEntry<I, T>>, Locator<G, BeanGroupEntry<I, T>>, Evictor<G>, Remover<G> {
    /**
     * Create a new bean group using the specified identifier and entry.
     * @param id a group identifier
     * @param entry the cache entry for the group
     * @return a bean group
     */
    BeanGroup<G, I, T> createGroup(G id, BeanGroupEntry<I, T> entry);
}
