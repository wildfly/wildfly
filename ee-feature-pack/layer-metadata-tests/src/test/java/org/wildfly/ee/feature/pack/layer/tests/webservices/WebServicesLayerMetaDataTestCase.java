/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.webservices;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class WebServicesLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationInRootJwsPackage() {
        testSingleWar(AnnotationInRootJwsPackageUsage.class);
    }

    @Test
    public void testAnnotationInJwsSoapPackage() {
        testSingleWar(AnnotationInJwsSoapPackageUsage.class);
    }

    @Test
    public void testAnnotationInRootXmlPackage() {
        testSingleWar(AnnotationInRootXmlPackageUsage.class);
    }

    @Test
    public void testAnnotationInXmlChildPackage() {
        testSingleWar(AnnotationInXmlChildPackageUsage.class);
    }

    @Test
    public void testClassInRootXmlPackage() {
        testSingleWar(ClassInRootXmlPackageUsage.class);
    }

    @Test
    public void testClassInXmlHandlerSoapPackage() {
        testSingleWar(ClassInXmlHandlerSoapPackageUsage.class);
    }

    @Test
    public void testClassInXmlHandlerPackage() {
        testSingleWar(ClassInXmlHandlerPackageUsage.class);
    }

    @Test
    public void testClassInXmlHttpPackage() {
        testSingleWar(ClassInXmlHttpPackageUsage.class);
    }

    @Test
    public void testClassInXmlSoapPackage() {
        testSingleWar(ClassInXmlSoapPackageUsage.class);
    }

    @Test
    public void testClassInXmlWsaddressingPackage() {
        testSingleWar(ClassInXmlWsaddressingPackageUsage.class);
    }

    private void testSingleWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("webservices", "webservices"));
    }

}
