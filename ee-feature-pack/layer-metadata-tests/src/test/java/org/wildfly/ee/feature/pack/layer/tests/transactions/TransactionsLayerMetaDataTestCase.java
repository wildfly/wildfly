/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.transactions;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class TransactionsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testTransactionAnnotationUsage() {
        // No nested packages for this one
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(TransactionAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testTransactionClassUsage() {
        // No nested packages for this one
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(TransactionClassUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("transactions", "transactions"));
    }
}
