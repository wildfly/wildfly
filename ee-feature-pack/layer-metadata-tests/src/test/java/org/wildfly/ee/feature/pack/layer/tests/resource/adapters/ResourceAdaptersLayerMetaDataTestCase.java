/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.resource.adapters;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ResourceAdaptersLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testRaXml() {
        Path p = createArchiveBuilder(ArchiveType.RAR)
                .addXml("ra.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testIronjacamarXml() {
        Path p = createArchiveBuilder(ArchiveType.RAR)
                .addXml("ironjacamar.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("resource-adapters", "resource-adapters"));
    }
}
