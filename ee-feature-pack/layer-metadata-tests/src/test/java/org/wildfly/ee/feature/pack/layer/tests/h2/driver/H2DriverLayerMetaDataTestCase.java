/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.h2.driver;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class H2DriverLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testH2DriverNonXaInWar() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-ds.xml", nonXaXml())
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testH2DriverNonXaInJar() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-ds.xml", nonXaXml())
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testH2DriverXaInWar() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("test-ds.xml", xaXml())
                .build();
        checkLayersForArchive(p);

    }

    @Test
    public void testH2DriverXaInJar() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("test-ds.xml", xaXml())
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testJakartaDataSourceDefinitionAnnotationUsage() throws Exception {
        testWarWithClass(JakartaDataSourceDefinitionAnnotationUsage.class);
    }

    @Test
    public void testJakartaDataSourceDefinitionAnnotationURLUsage() throws Exception {
        testWarWithClass(JakartaDataSourceDefinitionAnnotationURLUsage.class);
    }

    private void testWarWithClass(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        ExpectedLayers el = new ExpectedLayers();
        el.addDecorator("h2-driver");
        el.addLayer("h2-driver");
        el.addLayer("ee-integration");
        checkLayersForArchive(p,  el);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("h2-driver", "h2-driver"));
    }

    private String nonXaXml() {
        return createXmlElementWithContent("h2", "datasources", "datasource", "driver");
    }

    private String xaXml() {
        return createXmlElementWithContent("h2", "datasources", "xa-datasource", "driver");
    }
}
