/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jsf;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JsfLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testFacesServletInWebXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml(
                        "web.xml",
                        createXmlElementWithContent("jakarta.faces.webapp.FacesServlet", "web-app", "servlet", "servlet-class"))
                .build();
        checkLayersForArchive(p,
                new ExpectedLayers("jsf", "jsf")
                        // servlet is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
                        .addLayer("servlet"));
    }

    @Test
    public void testFacesConfigXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("faces-config.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testAnnotation() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassFromRootPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassFromNestedPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfClassFromNestedPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("jsf", "jsf"));
    }
}
