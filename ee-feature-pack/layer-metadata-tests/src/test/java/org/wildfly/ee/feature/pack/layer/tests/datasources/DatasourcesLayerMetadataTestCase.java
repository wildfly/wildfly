/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.datasources;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class DatasourcesLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJavaSqlUsageDetected() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JavaSqlUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testJavaxSqlUsageDetected() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JavaxSqlUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("datasources", "datasources"));
    }
}
