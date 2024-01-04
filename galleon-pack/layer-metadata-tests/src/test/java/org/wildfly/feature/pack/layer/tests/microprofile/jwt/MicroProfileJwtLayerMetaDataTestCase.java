/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.jwt;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileJwtLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAuthPackageAnnotation() {
        testSingleClassWar(MicroProfileJwtAuthPackageAnnotation.class);
    }

    @Test
    public void testJwtPackageAnnotation() {
        testSingleClassWar(MicroProfileJwtJwtPackageAnnotation.class);
    }

    @Test
    public void testJwtPackageClass() {
        testSingleClassWar(MicroProfileJwtJwtPackageClass.class);
    }

    @Test
    public void testJwtConfigPackageClass() {
        testSingleClassWar(MicroProfileJwtJwtConfigPackageAnnotation.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("microprofile-jwt", "microprofile-jwt"));
    }
}
