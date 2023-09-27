/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.chunk;

import java.io.Serializable;
import jakarta.batch.api.partition.PartitionCollector;
import jakarta.inject.Named;

@Named
public final class ChunkPartitionCollector implements PartitionCollector {
    @Override
    public Serializable collectPartitionData() throws Exception {
        return Thread.currentThread().getId();
    }
}
