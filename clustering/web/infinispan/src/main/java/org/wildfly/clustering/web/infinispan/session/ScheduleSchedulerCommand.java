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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Command that schedules a session.
 * @author Paul Ferraro
 */
public class ScheduleSchedulerCommand implements Command<Void, Scheduler> {
    private static final long serialVersionUID = -2606847692331278614L;

    private final transient ImmutableSession session;
    private final String id;
    private final long maxInactiveInterval;
    private final long lastAccessedTime;

    public ScheduleSchedulerCommand(ImmutableSession session) {
        this.session = session;
        this.id = session.getId();
        this.maxInactiveInterval = session.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS);
        this.lastAccessedTime = session.getMetaData().getLastAccessedTime().getTime();
    }

    @Override
    public Void execute(Scheduler scheduler) {
        ImmutableSession session = (this.session != null) ? this.session : new MockImmutableSession(this.id, new SimpleSessionMetaData(null, new Date(this.lastAccessedTime), new Time(this.maxInactiveInterval, TimeUnit.MILLISECONDS)));
        scheduler.schedule(session);
        return null;
    }
}
