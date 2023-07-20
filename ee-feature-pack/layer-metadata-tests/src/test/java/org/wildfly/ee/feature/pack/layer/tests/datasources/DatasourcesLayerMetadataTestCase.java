package org.wildfly.ee.feature.pack.layer.tests.datasources;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class DatasourcesLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJavaSqlUsageDetected() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JavaSqlUsage.class)
                .build();
        checkLayersForArchive(p, "datasources");
    }

    @Test
    public void testJavaxSqlUsageDetected() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JavaxSqlUsage.class)
                .build();
        checkLayersForArchive(p, "datasources");
    }
}
