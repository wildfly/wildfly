/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Decider;
import jakarta.batch.runtime.StepExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.wildfly.security.manager.WildFlySecurityManager;

@Named
public class Decider1 implements Decider {
    @Inject @BatchProperty(name = "decision-prop")
    private String decisionProp;

    @Inject @BatchProperty(name="reference-job-prop")
    private String referencingJobProp;

    @Inject @BatchProperty(name="reference-step-prop")
    private String referencingStepProp;  //not injected

    @Inject @BatchProperty(name = "reference-system-prop")
    private String referencingSystemProp;

    @Inject @BatchProperty(name = "reference-job-param")
    private String referencingJobParam;

    @Inject
    private JobContext jobContext;

    @Override
    public String decide(final StepExecution[] stepExecutions) throws Exception {
        Assert.assertEquals("decision-prop", decisionProp);
        Assert.assertEquals("job-prop", referencingJobProp);
        MatcherAssert.assertThat(referencingStepProp, not(equalTo("step-prop")));
        Assert.assertEquals(WildFlySecurityManager.getPropertyPrivileged("java.version", ""), referencingSystemProp);
        Assert.assertEquals("job-param", referencingJobParam);
        return "next";
    }
}
