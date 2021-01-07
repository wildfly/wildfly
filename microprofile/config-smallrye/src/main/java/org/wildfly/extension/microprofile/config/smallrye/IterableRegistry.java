/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A registry whose items can be iterated over.
 * @author Paul Ferraro
 */
public class IterableRegistry<T> implements Iterable<T>, Registry<T> {

    // Use a sorted map since we need a deterministic sort order. The order things are added to the SmallRyeConfigBuilder
    // has influence on the final sorting order, and we have tests checking this order is deterministic.
    private final Map<String, T> objects = new ConcurrentSkipListMap<>();

    @Override
    public void register(String name, T object) {
        this.objects.put(name, object);
    }

    @Override
    public void unregister(String name) {
        this.objects.remove(name);
    }

    @Override
    public Iterator<T> iterator() {
        return this.objects.values().iterator();
    }
}
