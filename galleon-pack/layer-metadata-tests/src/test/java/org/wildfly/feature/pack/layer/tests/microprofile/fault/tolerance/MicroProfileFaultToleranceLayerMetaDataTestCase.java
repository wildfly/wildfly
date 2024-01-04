/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.fault.tolerance;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileFaultToleranceLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationUsage() {
        // Only package containing annotations
        testSingleClassWar(MicroProfileFaultToleranceAnnotationUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() {
        testSingleClassWar(MicroProfileFaultToleranceClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromExceptionsPackageUsage() {
        testSingleClassWar(MicroProfileFaultToleranceClassFromExceptionsPackageUsage.class);
    }


    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("microprofile-fault-tolerance", "microprofile-fault-tolerance"));
    }
}
