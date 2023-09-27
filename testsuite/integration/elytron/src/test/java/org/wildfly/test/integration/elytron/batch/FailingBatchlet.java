/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.batch;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Jan Martiska
 */
@Named
public class FailingBatchlet implements Batchlet {

    @Inject
    @BatchProperty(name = "should.fail")
    private Boolean shouldFail;

    @Override
    public String process() throws Exception {
        if(shouldFail)
            throw new Exception("failing the job on purpose");
        return "OK";
    }

    @Override
    public void stop() throws Exception {

    }
}
