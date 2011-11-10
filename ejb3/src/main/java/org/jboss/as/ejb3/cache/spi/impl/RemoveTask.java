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

package org.jboss.as.ejb3.cache.spi.impl;

import org.jboss.as.ejb3.cache.Removable;
import org.jboss.logging.Logger;

/**
 * @author Paul Ferraro
 *
 */
public class RemoveTask<K> implements Runnable {
    private static final Logger log = Logger.getLogger(PassivateTask.class);

    private final Removable<K> cache;
    private final K id;

    public RemoveTask(Removable<K> cache, K id) {
        this.cache = cache;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            this.cache.remove(this.id);
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
        }
    }
}
