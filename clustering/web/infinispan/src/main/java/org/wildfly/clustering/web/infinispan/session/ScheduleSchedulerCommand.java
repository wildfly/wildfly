/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Command that schedules a session.
 * @author Paul Ferraro
 */
public class ScheduleSchedulerCommand implements Command<Void, Scheduler> {
    private static final long serialVersionUID = -2606847692331278614L;

    private transient ImmutableSessionMetaData metaData;
    private final String sessionId;

    public ScheduleSchedulerCommand(String sessionId, ImmutableSessionMetaData metaData) {
        this.sessionId = sessionId;
        this.metaData = metaData;
    }

    @Override
    public Void execute(Scheduler scheduler) {
        if (this.metaData != null) {
            scheduler.schedule(this.sessionId, this.metaData);
        } else {
            scheduler.schedule(this.sessionId);
        }
        return null;
    }
}
