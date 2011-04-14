/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.batch.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultBatchManager implements BatchManager {

    private Map<String, DefaultBatch> batches = Collections.emptyMap();
    private DefaultBatch activeBatch;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.batch.BatchManager#holdbackActiveBatch(java.lang.String)
     */
    @Override
    public boolean holdbackActiveBatch(String name) {
        if(activeBatch == null) {
            return false;
        }

        if(batches.containsKey(name)) {
            return false;
        }

        if(batches.isEmpty()) {
            batches = new HashMap<String, DefaultBatch>();
        }
        batches.put(name, activeBatch);
        activeBatch = null;
        return true;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.batch.BatchManager#discardActiveBatch()
     */
    @Override
    public boolean discardActiveBatch() {
        if(activeBatch == null) {
            return false;
        }
        activeBatch = null;
        return true;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.batch.BatchManager#getHeldbackNames()
     */
    @Override
    public Set<String> getHeldbackNames() {
        return batches.keySet();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.batch.BatchManager#getActiveBatch()
     */
    @Override
    public Batch getActiveBatch() {
        return activeBatch;
    }

    @Override
    public boolean isHeldback(String name) {
        return batches.containsKey(name);
    }

    @Override
    public boolean activateNewBatch() {
        if(activeBatch != null) {
            return false;
        }
        activeBatch = new DefaultBatch();
        return true;
    }

    @Override
    public boolean isBatchActive() {
        return activeBatch != null;
    }

    @Override
    public boolean activateHeldbackBatch(String name) {
        if(activeBatch != null) {
            return false;
        }
        if(!batches.containsKey(name)) {
            return false;
        }

        activeBatch = batches.remove(name);
        return activeBatch != null;
    }
}
