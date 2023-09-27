/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.rest.client;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileRestClientLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testClassFromRootPackage() {
        testSingleClassWar(MicroProfileRestClientClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromExtPackage() {
        testSingleClassWar(MicroProfileRestClientClassFromExtPackageUsage.class);
    }

    @Test
    public void testClassFromSpiPackage() {
        testSingleClassWar(MicroProfileRestClientClassFromSpiPackageUsage.class);
    }

    @Test
    public void testAnnotationFromAnnotationsPackage() {
        testSingleClassWar(MicroProfileRestClientAnnotationFromAnnotationsPackageUsage.class);
    }

    @Test
    public void testAnnotationFromInjectPackage() {
        testSingleClassWar(MicroProfileRestClientAnnotationFromInjectPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("microprofile-rest-client", "microprofile-rest-client"));
    }
}
