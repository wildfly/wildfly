package org.wildfly.ee.feature.pack.layer.tests.embedded.activemq;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EmbeddedActiveMqLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJmsDestinationsInJmxXmlInWar() throws Exception {
        String xml = createXml();
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-jms.xml", xml)
                .build();
        checkLayersForArchive(p, "embedded-activemq");
    }

    @Test
    public void testJmsDestinationsInJmxXmlInJar() throws Exception {
        String xml = createXml();
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-jms.xml", xml)
                .build();
        checkLayersForArchive(p, "embedded-activemq");
    }


    private String createXml() {
        return createXmlElementWithContent(null, "messaging-deployment", "server", "jms-destinations");
    }
}
