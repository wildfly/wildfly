/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan.scheduler;

import org.wildfly.clustering.dispatcher.Command;

/**
 * @author Paul Ferraro
 */
public class ContainsCommand<I, M> implements Command<Boolean, CacheEntryScheduler<I, M>> {
    private static final long serialVersionUID = 7221762541453484399L;

    private final I id;

    ContainsCommand(I id) {
        this.id = id;
    }

    @Override
    public Boolean execute(CacheEntryScheduler<I, M> scheduler) throws Exception {
        return scheduler.contains(this.id);
    }

    I getId() {
        return this.id;
    }
}
