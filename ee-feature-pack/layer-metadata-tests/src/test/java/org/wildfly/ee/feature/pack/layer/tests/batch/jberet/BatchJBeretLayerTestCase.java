/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.batch.jberet;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

public class BatchJBeretLayerTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testBatchJBeretDetectedFileInWar() throws Exception {
        // Not sure how to add an empty folder with shrinkwrap so do this manually
        Path p = ARCHIVES_PATH.resolve("batch-jberet-file.war");
        Files.createDirectories(p.resolve("WEB-INF/classes/META-INF/batch-jobs"));
        checkLayersForArchive(p);
    }

    @Test
    public void testBatchJBeretClassInRootPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(BatchClassFromApiPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testBatchJBeretClassInChunkPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(BatchClassInApiChunkPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testBatchJBeretInChunkListenerPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(BatchClassInApiChunkListenerPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testBatchJBeretInListenerPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(BatchClassInApiListenerPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testBatchJBeretInPartitionPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(BatchClassInApiPartitionPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("batch-jberet", "batch-jberet"));
    }

}
