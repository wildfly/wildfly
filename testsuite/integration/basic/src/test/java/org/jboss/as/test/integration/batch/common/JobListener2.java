/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Assert;

@Named("L2")
public class JobListener2 implements JobListener {
    @Inject @BatchProperty(name="job-prop")
    private String jobProp = "L2";  //unmatched property

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //nothing is injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referencedProp;  //nothing is injected

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        //Assert.assertEquals("L2", jobProp);  should be null or "L2"?
        Assert.assertEquals(null, listenerProp);
        Assert.assertEquals(null, referencedProp);
        Assert.assertEquals(2, jobContext.getProperties().size());
        Assert.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
    }
}
