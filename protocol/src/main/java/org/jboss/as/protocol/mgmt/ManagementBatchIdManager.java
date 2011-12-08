/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
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
 */
package org.jboss.as.protocol.mgmt;

import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for generating new unique batch ids on the server side
 * of a channel. The batch ids are used to group several individual channel requests
 * that make up a larger use case.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ManagementBatchIdManager {

    /**
     * Block a given batch id, when using shared transports.
     *
     * @param id the id
     * @return true if this did not already contain the id
     */
    boolean lockBatchId(int id);

    /**
     * Creates a batch id. Once the batch has completed
     * {@link ManagementBatchIdManager#freeBatchId} must be called.
     *
     * @return the created batch id
     */
    int createBatchId();

    /**
     * Frees a batch id.
     *
     * @param id the batch id to be freed.
     */
    void freeBatchId(int id);

    class DefaultManagementBatchIdManager implements ManagementBatchIdManager {

        private final Set<Integer> ids = new HashSet<Integer>();

        @Override
        public synchronized boolean lockBatchId(int id) {
            if(ids.contains(id)) {
                return false;
            }
            ids.add(id);
            return true;
        }

        @Override
        public synchronized int createBatchId() {
            int next = (int)(Math.random() * Integer.MAX_VALUE);
            while (ids.contains(next)) {
                next = (int)(Math.random() * Integer.MAX_VALUE);
            }
            ids.add(next);
            return next;
        }

        @Override
        public synchronized void freeBatchId(int id) {
            ids.remove(id);
        }

    };
}
