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

import java.util.Map;

import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * The cache entry for a bean group
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public interface BeanGroupEntry<I, T> {

    /**
     * Returns the beans in this group indexed via identifier.
     * @return a marshalled value
     */
    MarshalledValue<Map<I, T>, MarshallingContext> getBeans();

    /**
     * Increments the usage count of the specified bean.
     * @param id a bean identifier
     * @return the previous usage count
     */
    int incrementUsage(I id);

    /**
     * Decrements the usage count of the specified bean.
     * @param id a bean identifier
     * @return the current usage count
     */
    int decrementUsage(I id);

    /**
     * The total usage counts for all beans in this group/
     * @return the total usage count
     */
    int totalUsage();
}
