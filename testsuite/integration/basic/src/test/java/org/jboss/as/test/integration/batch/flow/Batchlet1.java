/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.flow;

import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.as.test.integration.batch.common.Batchlet0;
import org.junit.Assert;

@Named
public class Batchlet1 extends Batchlet0 {
    @Inject
    @BatchProperty(name = "reference-step-prop")
    private String referencingStepProp;

    @Override
    public String process() throws Exception {
        final String result = super.process();

        final String stepToVerify = "step1";
        if (stepContext.getStepName().equals(stepToVerify)) {
            Assert.assertEquals("step-prop", referencingStepProp);
        }

        return result;
    }
}
