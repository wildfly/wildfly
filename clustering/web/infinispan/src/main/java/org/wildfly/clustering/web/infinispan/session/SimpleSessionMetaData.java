/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Basic session meta data implementation.
 * @author Paul Ferraro
 */
public class SimpleSessionMetaData implements SessionMetaData {

    private final Date creationTime;
    private volatile Date lastAccessedTime;
    private volatile Time maxInactiveInterval;

    public SimpleSessionMetaData() {
        Date now = new Date();
        this.creationTime = now;
        this.lastAccessedTime = now;
        this.maxInactiveInterval = new Time(0, TimeUnit.MILLISECONDS);
    }

    public SimpleSessionMetaData(Date creationTime, Date lastAccessedTime, Time maxInactiveInterval) {
        this.creationTime = creationTime;
        this.lastAccessedTime = lastAccessedTime;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public boolean isExpired() {
        long maxInactiveInterval = this.getMaxInactiveInterval(TimeUnit.MILLISECONDS);
        return (maxInactiveInterval > 0) ? (System.currentTimeMillis() - this.lastAccessedTime.getTime()) >= maxInactiveInterval : false;
    }

    @Override
    public boolean isNew() {
        // Identity comparison is intentional
        return this.lastAccessedTime == this.creationTime;
    }

    @Override
    public Date getCreationTime() {
        return this.creationTime;
    }

    @Override
    public Date getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setLastAccessedTime(Date date) {
        this.lastAccessedTime = date;
    }

    @Override
    public long getMaxInactiveInterval(TimeUnit unit) {
        return this.maxInactiveInterval.convert(unit);
    }

    @Override
    public void setMaxInactiveInterval(long interval, TimeUnit unit) {
        this.maxInactiveInterval = new Time(interval, unit);
    }
}
