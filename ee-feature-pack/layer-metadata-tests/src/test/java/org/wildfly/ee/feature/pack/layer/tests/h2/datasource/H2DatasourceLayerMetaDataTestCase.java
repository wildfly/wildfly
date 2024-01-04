/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.h2.datasource;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class H2DatasourceLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testPersistenceXmlEntryInWebInfClasesMetaInf() {
        String xml = createXmlElementWithContent("java:jboss/datasources/ExampleDS", "persistence", "persistence-unit", "jta-data-source");
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("persistence.xml", xml, true)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testPersistenceXmlEntryInMetaInf() {
        String xml = createXmlElementWithContent("java:jboss/datasources/ExampleDS", "persistence", "persistence-unit", "jta-data-source");
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("persistence.xml", xml)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p,
                new ExpectedLayers("h2-datasource", "h2-datasource")
                        .addLayerAndDecorator("jpa", "jpa"));

    }
}
