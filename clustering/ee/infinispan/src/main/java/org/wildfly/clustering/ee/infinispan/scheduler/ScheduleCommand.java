/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
 * Command that scheduled an item.
 * @author Paul Ferraro
 */
public class ScheduleCommand<I, M> implements Command<Void, Scheduler<I, M>> {
    private static final long serialVersionUID = 6254782388444864112L;

    private final I id;
    private final transient M metaData;

    public ScheduleCommand(I id, M metaData) {
        this.id = id;
        this.metaData = metaData;
    }

    ScheduleCommand(I id) {
        this(id, null);
    }

    I getId() {
        return this.id;
    }

    @Override
    public Void execute(Scheduler<I, M> scheduler) {
        if (this.metaData != null) {
            scheduler.schedule(this.id, this.metaData);
        } else {
            scheduler.schedule(this.id);
        }
        return null;
    }
}
