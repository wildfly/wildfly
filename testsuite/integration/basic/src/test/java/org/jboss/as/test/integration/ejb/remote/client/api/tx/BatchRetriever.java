/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

/**
 * User: jpai
 */
public interface BatchRetriever {
    Batch fetchBatch(final String batchName);
}
