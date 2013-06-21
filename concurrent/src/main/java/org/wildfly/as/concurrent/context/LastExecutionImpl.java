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
package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.LastExecution;
import java.util.Date;

/**
 * A simple POJO kind impl for {@link LastExecution}.
 *
 * @author Eduardo Martins
 */
public class LastExecutionImpl implements LastExecution {

    private String identityName;
    private Object result;
    private Date runEnd;
    private Date runStart;
    private Date scheduledStart;
    private boolean skipped;

    @Override
    public String getIdentityName() {
        return identityName;
    }

    /**
     * @param identityName
     */
    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    @Override
    public Object getResult() {
        return result;
    }

    /**
     * @param result
     */
    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public Date getRunEnd() {
        return runEnd;
    }

    /**
     * @param runEnd
     */
    public void setRunEnd(Date runEnd) {
        this.runEnd = runEnd;
    }

    @Override
    public Date getRunStart() {
        return runStart;
    }

    /**
     * @param runStart
     */
    public void setRunStart(Date runStart) {
        this.runStart = runStart;
    }

    @Override
    public Date getScheduledStart() {
        return scheduledStart;
    }

    /**
     * @param scheduledStart
     */
    public void setScheduledStart(Date scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    /**
     * @return
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * @param skipped
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}
