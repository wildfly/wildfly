/*
 * Copyright (c) 2012-2013 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jboss.as.test.integration.batch.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;

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
