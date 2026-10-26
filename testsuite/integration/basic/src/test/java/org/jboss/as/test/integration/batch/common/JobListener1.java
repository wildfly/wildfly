/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.AbstractJobListener;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.wildfly.security.manager.WildFlySecurityManager;

import org.junit.jupiter.api.Assertions;

@Named("L1")
public class JobListener1 extends AbstractJobListener implements JobListener {
    @Inject @BatchProperty(name="job-prop")
    private String jobProp;  //nothing is injected

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referenceJobProp;

    @Inject @BatchProperty(name="reference-job-param")
    private String referenceJobParam;

    @Inject @BatchProperty(name="reference-system-property")
    private String referenceSystemProperty;

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        Assertions.assertEquals(null, jobProp);
        Assertions.assertEquals("listener-prop", listenerProp);
        Assertions.assertEquals("job-prop", referenceJobProp);
        Assertions.assertEquals("job-param", referenceJobParam);
        Assertions.assertEquals(WildFlySecurityManager.getPropertyPrivileged("java.version", ""), referenceSystemProperty);

        Assertions.assertEquals(2, jobContext.getProperties().size());
        Assertions.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
    }
}
