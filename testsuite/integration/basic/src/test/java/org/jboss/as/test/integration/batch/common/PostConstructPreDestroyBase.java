/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

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
