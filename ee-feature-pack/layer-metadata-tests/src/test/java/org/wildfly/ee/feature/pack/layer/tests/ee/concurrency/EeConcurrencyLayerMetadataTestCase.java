/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ee.concurrency;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeConcurrencyLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testEeConcurrencyUsageDetected() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(EeConcurrencyClassUsage.class)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("ee-concurrency", "ee-concurrency"));
    }

}
