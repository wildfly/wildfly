/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.component.interceptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.Integer.toHexString;

import org.jboss.as.ee.logging.EeLogger;

/**
 * Container for an ordered list of object. Objects are added to the container, and can be sorted and
 * retrieved via {@link #getSortedItems()}. In order to prevent excessive sorts once the sort has been performed
 * no new objects can be added.
 * <p/>
 * The sort is guaranteed to be stable, so adding multiple objects with the same priority means that they will be
 * run in the order that they were added.
 *
 * @author Stuart Douglas
 */
public class OrderedItemContainer<T> {

    private final Map<Integer, T> items = new HashMap<Integer, T>();
    private volatile List<T> sortedItems;

    public void add(final T item, int priority) {
        if(sortedItems != null) {
            throw EeLogger.ROOT_LOGGER.cannotAddMoreItems();
        }
        if(item == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("item");
        }
        final T current = items.get(priority);
        if (current != null) {
            throw EeLogger.ROOT_LOGGER.priorityAlreadyExists(item, toHexString(priority), current);
        }
        items.put(priority, item);
    }

    public List<T> getSortedItems() {
        if(sortedItems == null) {
            final SortedMap<Integer, T> sortedMap = new TreeMap<Integer, T>(items);
            sortedItems = new ArrayList<T>(sortedMap.values());
        }
        return sortedItems;
    }

}
