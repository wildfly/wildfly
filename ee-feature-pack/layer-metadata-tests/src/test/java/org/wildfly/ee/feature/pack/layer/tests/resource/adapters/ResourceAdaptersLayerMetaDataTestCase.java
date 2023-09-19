package org.wildfly.ee.feature.pack.layer.tests.resource.adapters;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ResourceAdaptersLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testRaXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.RAR)
                .addXml("ra.xml", "")
                .build();
        checkLayersForArchive(p, "resource-adapters");
    }

    @Test
    public void testIronjacamarXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.RAR)
                .addXml("ironjacamar.xml", "")
                .build();
        checkLayersForArchive(p, "resource-adapters");
    }
}
