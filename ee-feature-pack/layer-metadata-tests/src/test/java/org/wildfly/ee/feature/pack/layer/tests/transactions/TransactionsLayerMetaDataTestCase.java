package org.wildfly.ee.feature.pack.layer.tests.transactions;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class TransactionsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testTransactionAnnotationUsage() throws Exception {
        // No nested packages for this one
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(TransactionAnnotationUsage.class)
                .build();
        checkLayersForArchive(p, "transactions");
    }

    @Test
    public void testTransactionClassUsage() throws Exception {
        // No nested packages for this one
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(TransactionClassUsage.class)
                .build();
        checkLayersForArchive(p, "transactions");
    }
}
