package org.wildfly.ee.feature.pack.layer.tests.naming;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class NamingLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    javax.naming.Context context;

    @Test
    public void testNamingClassUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(this.getClass())
                .build();
        checkLayersForArchive(p, "naming");
    }

}
