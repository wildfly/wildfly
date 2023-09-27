/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.servlet;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ServletLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testServletAnnotation() {
        // No sub packages for this one
        testSingleClassWar(ServletAnnotationUsage.class);
    }

    @Test
    public void testWebsocketAnnotationFromRootPackage() {
        testSingleClassWar(WebsocketAnnotationFromRootPackageUsage.class);
    }

    @Test
    public void testWebsocketAnnotationFromServerPackage() {
        testSingleClassWar(WebsocketAnnotationFromServerPackageUsage.class);
    }

    @Test
    public void testServletInWebXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", createXmlElementWithContent("", "web-app", "servlet"))
                .build();
        // servlet is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "servlet");
    }

    @Test
    public void testServletClassFromRootPackage() {
        testSingleClassWar(ServletClassFromRootPackageUsage.class);
    }

    @Test
    public void testServletClassFromDescriptorPackage() {
        testSingleClassWar(ServletClassFromDescriptorPackageUsage.class);
    }

    @Test
    public void testServletClassFromHttpPackage() {
        testSingleClassWar(ServletClassFromDescriptorPackageUsage.class);
    }

    @Test
    public void testWebsocketClassFromRootPackage() {
        testSingleClassWar(WebSocketClassFromRootPackageUsage.class);
    }

    @Test
    public void testWebsocketClassFromServerPackage() {
        testSingleClassWar(WebSocketClassFromServerPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        // servlet is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "servlet");
    }
}
