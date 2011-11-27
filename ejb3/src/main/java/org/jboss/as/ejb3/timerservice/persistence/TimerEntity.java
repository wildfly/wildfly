/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.persistence;

import java.io.Serializable;
import java.util.Date;

import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerState;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Stuart Douglas
 */
public class TimerEntity implements Serializable {

    protected String id;

    protected String timedObjectId;

    protected Date initialDate;

    protected long repeatInterval;

    protected Date nextDate;

    protected Date previousRun;

    protected Serializable info;

    protected Object primaryKey;

    protected TimerState timerState;

    public TimerEntity() {

    }

    public TimerEntity(TimerImpl timer) {
        this.id = timer.getId();
        this.initialDate = timer.getInitialExpiration();
        this.repeatInterval = timer.getInterval();
        this.nextDate = timer.getNextExpiration();
        this.previousRun = timer.getPreviousRun();
        this.timerState = timer.getState();
        this.timedObjectId = timer.getTimedObjectId();
        this.info = timer.getTimerInfo();
        this.primaryKey = timer.getPrimaryKey();
    }

    public String getId() {
        return id;
    }

    public String getTimedObjectId() {
        return timedObjectId;
    }

    public Date getInitialDate() {
        return initialDate;
    }

    public long getInterval() {
        return repeatInterval;
    }

    public Serializable getInfo() {
        return this.info;
    }

    public Date getNextDate() {
        return nextDate;
    }

    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    public Date getPreviousRun() {
        return previousRun;
    }

    public void setPreviousRun(Date previousRun) {
        this.previousRun = previousRun;
    }

    public TimerState getTimerState() {
        return timerState;
    }

    public void setTimerState(TimerState timerState) {
        this.timerState = timerState;
    }

    public boolean isCalendarTimer() {
        return false;
    }

    public Object getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(final Object primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TimerEntity)) {
            return false;
        }
        TimerEntity other = (TimerEntity) obj;
        if (this.id == null) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            return super.hashCode();
        }
        return this.id.hashCode();
    }

}
