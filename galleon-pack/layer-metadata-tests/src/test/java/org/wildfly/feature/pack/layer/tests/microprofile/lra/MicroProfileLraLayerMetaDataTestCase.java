/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.lra;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileLraLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testClassFromRootPackage() {
        testSingleClassWar(MicroProfileLraClassFromRootPackageUsage.class);
    }


    @Test
    public void testAnnotationFromAnnotationPackage() {
        testSingleClassWar(MicroProfileLraAnnotationFromAnnotationPackageUsage.class);
    }

    @Test
    public void testClassFromAnnotationPackage() {
        testSingleClassWar(MicroProfileLraClassFromAnnotationPackageUsage.class);
    }

    @Test
    public void testAnnotationFromAnnotationWsRsPackage() {
        testSingleClassWar(MicroProfileLraAnnotationFromAnnotationWsRsPackageUsage.class);
    }


    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(AbstractLayerMetaDataTestCase.ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("microprofile-lra-participant", "microprofile-lra-participant"));
    }
}
