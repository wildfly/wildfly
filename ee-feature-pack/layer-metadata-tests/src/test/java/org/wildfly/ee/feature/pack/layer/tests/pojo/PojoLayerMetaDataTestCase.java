package org.wildfly.ee.feature.pack.layer.tests.pojo;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class PojoLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJarMetaInfBeansXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("one-jboss-beans.xml", "")
                .build();
        checkLayersForArchive(p, "pojo");
    }

    @Test
    public void testWarWebInfBeansXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("xjboss-beans.xml", "")
                .build();
        checkLayersForArchive(p, "pojo");
    }

    @Test
    public void testWarWebInfClassesMetaInfBeansXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("jboss-beans.xml", "", true)
                .build();
        checkLayersForArchive(p, "pojo");
    }
}
