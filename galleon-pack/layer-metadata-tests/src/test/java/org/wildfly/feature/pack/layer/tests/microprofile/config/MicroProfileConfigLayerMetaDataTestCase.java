/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.config;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileConfigLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationUsage() {
        // Only package containing annotations
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassFromRootPackageUsage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassFromSpiPackageUsage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigClassFromSpiPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testMetaInfMicroProfileConfigProperties() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addFileToInf("microprofile-config.properties", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testWebInfClassesMetaInfMicroProfileConfigProperties(){
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addFileToInf("microprofile-config.properties", "", true)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("microprofile-config", "microprofile-config"));
    }
}
