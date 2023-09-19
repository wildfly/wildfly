package org.wildfly.ee.feature.pack.layer.tests.ee.concurrency;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeConcurrencyLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testEeConcurrencyUsageDetected() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(EeConcurrencyClassUsage.class)
                .build();
        checkLayersForArchive(p, "ee-concurrency");
    }

}
