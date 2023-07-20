package org.wildfly.ee.feature.pack.layer.tests.cdi;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.JAR;
import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

public class CdiLayerTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testCdiDetectedFileInWar() throws Exception {
        Path p = createArchiveBuilder(WAR)
                .addXml("beans.xml", "")
                .build();
        checkLayersForArchive(p, "cdi");
    }

    @Test
    public void testCdiDetectedFileInJar() throws Exception {
        Path p = createArchiveBuilder(JAR)
                .addXml("beans.xml", "")
                .build();
        checkLayersForArchive(p, "cdi");
    }

    @Test
    public void testInjectPackage() throws Exception {
        Path p = createArchiveBuilder(WAR)
                .addClasses(CdiInjectClass.class)
                .build();
        checkLayersForArchive(p, "cdi");
    }

    @Test
    public void testEnterpriseContext() throws Exception {
        //This is in a sub-package of jakarta.enterprise.context
        Path p = createArchiveBuilder(WAR)
                .addClasses(CdiEnterpriseContextClass.class)
                .build();
        checkLayersForArchive(p, "cdi");
    }

}
