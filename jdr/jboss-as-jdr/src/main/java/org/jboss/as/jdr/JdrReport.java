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

package org.jboss.as.jdr;

import java.util.Date;

/**
 * Provides metadata about and access to the data collected by a {@link JdrReportCollector}.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class JdrReport {
    private Date startTime;
    private Date endTime;
    private String location;

    public JdrReport() {
    }

    /**
     * Indicates the time the JDR report collection was initiated.
     */
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date time) {
        startTime = time;
    }

    public void setStartTime() {
        setStartTime(new Date());
    }

    /**
     * Indicates the time the JDR report collection was complete.
     */
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date time) {
        endTime = time;
    }

    public void setEndTime() {
        setEndTime(new Date());
    }

    /**
     * Indicates the location of the generated JDR report.
     *
     * @return location of report.
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
