/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence;

/**
 * Formats a cache key to a string representation and back again.
 * @author Paul Ferraro
 */
public interface KeyFormat<K> {
    /**
     * The implementation class of the target key of this format.
     * @return an implementation class
     */
    Class<K> getTargetClass();

    /**
     * Parses the key from the specified string.
     * @param value a string representation of the key
     * @return the parsed key
     */
    K parse(String value);

    /**
     * Formats the specified key to a string representation.
     * @param key a key to format
     * @return a string representation of the specified key.
     */
    String format(K key);
}
