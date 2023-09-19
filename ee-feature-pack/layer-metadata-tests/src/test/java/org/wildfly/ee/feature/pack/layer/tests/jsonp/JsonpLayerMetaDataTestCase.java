package org.wildfly.ee.feature.pack.layer.tests.jsonp;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JsonpLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testClassFromRootPackageUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsonpClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jsonp");
    }

    @Test
    public void testClassFromStreamPackageUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsonpClassFromStreamPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jsonp");
    }
}
