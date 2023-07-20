package org.wildfly.ee.feature.pack.layer.tests.h2.datasource;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class H2DatasourceLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testPersistenceXmlEntryInWebInfClasesMetaInf() throws Exception {
        String xml = createXmlElementWithContent("java:jboss/datasources/ExampleDS", "persistence", "persistence-unit", "jta-data-source");
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("persistence.xml", xml, true)
                .build();
        checkLayersForArchive(p, "h2-datasource", "jpa");
    }

    @Test
    public void testPersistenceXmlEntryInMetaInf() throws Exception {
        String xml = createXmlElementWithContent("java:jboss/datasources/ExampleDS", "persistence", "persistence-unit", "jta-data-source");
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("persistence.xml", xml)
                .build();
        checkLayersForArchive(p, "h2-datasource", "jpa");
    }
}
