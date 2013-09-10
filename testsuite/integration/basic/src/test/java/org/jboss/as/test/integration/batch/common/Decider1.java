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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

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
        Assert.assertThat(referencingStepProp, not(equalTo("step-prop")));
        Assert.assertEquals(WildFlySecurityManager.getPropertyPrivileged("java.version", ""), referencingSystemProp);
        Assert.assertEquals("job-param", referencingJobParam);
        return "next";
    }
}
