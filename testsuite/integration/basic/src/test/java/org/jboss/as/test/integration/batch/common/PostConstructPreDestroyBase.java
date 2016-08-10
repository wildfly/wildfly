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

package org.jboss.as.test.integration.batch.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public abstract class PostConstructPreDestroyBase {
    @Inject
    protected JobContext jobContext;

    @Inject
    protected StepContext stepContext;

    private boolean allowAddToJobExitStatus;  //set in job-level property add.to.job.exit.status in job xml

    @PostConstruct
    private void ps() {
        final String p = jobContext.getProperties().getProperty("add.to.job.exit.status");
        allowAddToJobExitStatus = Boolean.parseBoolean(p);
        addToJobExitStatus("PostConstructPreDestroyBase.ps");
    }

    @PreDestroy
    private void pd() {
        addToJobExitStatus("PostConstructPreDestroyBase.pd");
    }

    protected void addToJobExitStatus(final String s) {
        if (allowAddToJobExitStatus) {
            final String jes = jobContext.getExitStatus();
            if (jes == null) {
                jobContext.setExitStatus(s);
            } else {
                jobContext.setExitStatus(jes + " " + s);
            }
        }
    }
}
