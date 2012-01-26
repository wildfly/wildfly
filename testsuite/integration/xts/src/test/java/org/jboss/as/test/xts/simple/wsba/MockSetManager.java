/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.simple.wsba;

import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a simple Set collection.
 *
 * @author paul.robinson@redhat.com, 2011-12-21
 */
public class MockSetManager {
    private static final Logger log = Logger.getLogger(MockSetManager.class);

    private static final Set<String> set = new HashSet<String>();

    /**
     * Add a value to the set
     * 
     * @param item Item to add to the set.
     * @throws AlreadyInSetException if the item is already in the set.
     */
    public static void add(String item) throws AlreadyInSetException {
        synchronized (set) {

            if (set.contains(item)) {
                throw new AlreadyInSetException("item '" + item + "' is already in the set.");
            }
            set.add(item);
        }
    }

    /**
     * Persist sufficient data, such that the add operation can be undone or made permanent when told to do so by a call to
     * commit or rollback.
     * 
     * As this is a mock implementation, the method does nothing and always returns true.
     * 
     * @return true if the SetManager is able to commit and the required state was persisted. False otherwise.
     */
    public static boolean prepare() {
        return true;
    }

    /**
     * Make the outcome of the add operation permanent.
     * 
     * As this is a mock implementation, the method does nothing.
     */
    public static void commit() {
        log.info("[SERVICE] Commit the backend resource (e.g. commit any changes to databases so that they are visible to others)");
    }

    /**
     * Undo any changes made by the add operation.
     * 
     * As this is a mock implementation, the method needs to be informed of how to undo the work of the add operation. Typically
     * resource managers will already know this information.
     * 
     * @param item The item to remove from the set in order to undo the effects of the add operation.
     */
    public static void rollback(String item) {
        log.info("[SERVICE] Compensate the backend resource by removing '" + item
                + "' from the set (e.g. undo any changes to databases that were previously made visible to others)");
        synchronized (set) {

            set.remove(item);
        }

    }

    /**
     * Query the set to see if it contains a particular value.
     * 
     * @param value the value to check for.
     * @return true if the value was present, false otherwise.
     */
    public static boolean isInSet(String value) {
        return set.contains(value);
    }

    /**
     * Empty the set
     */
    public static void clear() {
        set.clear();
    }
}
