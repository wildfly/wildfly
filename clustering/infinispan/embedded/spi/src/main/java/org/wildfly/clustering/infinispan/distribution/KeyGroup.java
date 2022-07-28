/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.distribution;

/**
 * Implemented by keys that should be logical grouped by a common identifier.
 * Keys with the same group identifier will be stored within the same segment.
 * This is analogous to Infinispan's {@link org.infinispan.distribution.group.Group} logic, but avoids a potentially unnecessary String conversion.
 * @author Paul Ferraro
 */
public interface KeyGroup<I> {
    /**
     * The identifier of this group of keys.
     * @return an group identifier
     */
    I getId();
}
