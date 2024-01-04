/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

/**
 * User: jpai
 */
public interface RemoteBatch {

    void createBatch(final String batchName);

    void step1(final String batchName, final String stepName);

    void successfulStep2(final String batchName, final String stepName);

    void appExceptionFailingStep2(final String batchName, final String stepName) throws SimpleAppException;

    void systemExceptionFailingStep2(final String batchName, final String stepName);

    void independentStep3(final String batchName, final String stepName);

    void step4(final String batchName, final String stepName);
}
