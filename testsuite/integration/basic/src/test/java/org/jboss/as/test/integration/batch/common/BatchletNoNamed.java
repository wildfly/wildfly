/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.inject.Inject;

public class BatchletNoNamed extends PostConstructPreDestroyBase implements Batchlet {
    @Inject
    @BatchProperty(name = "batchlet-prop")
    protected String batchletProp;

    @Inject
    @BatchProperty(name = "reference-job-prop")
    protected String referencingJobProp;

    @Inject
    @BatchProperty(name = "reference-system-prop")
    protected String referencingSystemProp;

    @Inject
    @BatchProperty(name = "reference-job-param")
    protected String referencingJobParam;

    @Override
    public String process() throws Exception {
        return "Processed";
    }

    @Override
    public void stop() throws Exception {
    }

    @PostConstruct
    void ps() {
        addToJobExitStatus("BatchletNoNamed.ps");
    }

    @PreDestroy
    void pd() {
        addToJobExitStatus("BatchletNoNamed.pd");
    }

}
