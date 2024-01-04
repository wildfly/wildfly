/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jaxrs;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JaxrsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJaxrsAnnotationFromRootPackage() {
        testOneClassInWar(JaxrsRootPackageAnnotationUsage.class);
    }

    @Test
    public void testJaxrsClassFromRootPackage() {
        testOneClassInWar(JaxrsRootPackageClassUsage.class);
    }

    @Test
    public void testJaxrsAnnotationFromCorePackage() {
        testOneClassInWar(JaxrsCorePackageAnnotationUsage.class);
    }

    @Test
    public void testJaxrsClassFromCorePackage() {
        testOneClassInWar(JaxrsCorePackageClassUsage.class);
    }

    @Test
    public void testApplicationInWebXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml(
                        "web.xml",
                        createXmlElementWithContent("jakarta.ws.rs.core.Application", "web-app", "servlet", "servlet-class"))
                .build();
        checkLayersForArchive(p,
                new ExpectedLayers("jaxrs", "jaxrs")
                        // servlet is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
                        .addLayer("servlet"));
    }

    private void testOneClassInWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("jaxrs", "jaxrs"));
    }

}
