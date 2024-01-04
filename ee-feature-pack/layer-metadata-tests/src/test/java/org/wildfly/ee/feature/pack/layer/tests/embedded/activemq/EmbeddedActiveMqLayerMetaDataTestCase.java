/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.embedded.activemq;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EmbeddedActiveMqLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJmsDestinationsInJmxXmlInWar() {
        String xml = createXml();
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-jms.xml", xml)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testJmsDestinationsInJmxXmlInJar() {
        String xml = createXml();
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-jms.xml", xml)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("embedded-activemq", "embedded-activemq"));
    }

    private String createXml() {
        return createXmlElementWithContent(null, "messaging-deployment", "server", "jms-destinations");
    }
}
