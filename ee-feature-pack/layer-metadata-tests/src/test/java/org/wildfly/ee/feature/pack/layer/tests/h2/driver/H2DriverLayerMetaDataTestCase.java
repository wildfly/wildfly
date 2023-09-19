package org.wildfly.ee.feature.pack.layer.tests.h2.driver;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class H2DriverLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testH2DriverNonXaInWar() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-ds.xml", nonXaXml())
                .build();
        checkLayersForArchive(p, "h2-driver");
    }

    @Test
    public void testH2DriverNonXaInJar() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-ds.xml", nonXaXml())
                .build();
        checkLayersForArchive(p, "h2-driver");
    }

    @Test
    public void testH2DriverXaInWar() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-ds.xml", xaXml())
                .build();
        checkLayersForArchive(p, "h2-driver");

    }

    @Test
    public void testH2DriverXaInJar() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-ds.xml", xaXml())
                .build();
        checkLayersForArchive(p, "h2-driver");
    }

    private String nonXaXml() {
        return createXmlElementWithContent("h2", "datasources", "datasource", "driver");
    }

    private String xaXml() {
        return createXmlElementWithContent("h2", "datasources", "xa-datasource", "driver");
    }
}
