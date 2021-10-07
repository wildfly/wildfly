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
 * Command that scheduled an element.
 * @author Paul Ferraro
 */
public interface ScheduleCommand<I, M> extends Command<Void, Scheduler<I, M>> {

    /**
     * Returns the identifier of the element to be scheduled.
     * @return the identifier of the element to be scheduled.
     */
    I getId();

    /**
     * Returns the meta data of the element to be scheduled.
     * @return the meta data of the element to be scheduled.
     */
    M getMetaData();

    @Override
    default Void execute(Scheduler<I, M> scheduler) throws Exception {
        I id = this.getId();
        M metaData = this.getMetaData();
        if (metaData != null) {
            scheduler.schedule(id, metaData);
        } else {
            scheduler.schedule(id);
        }
        return null;
    }
}
