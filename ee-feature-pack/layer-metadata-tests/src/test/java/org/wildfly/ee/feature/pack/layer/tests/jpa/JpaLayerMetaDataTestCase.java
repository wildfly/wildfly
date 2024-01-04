/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jpa;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JpaLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testMetaInfPersistenceXml() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("persistence.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testWebInfClassesMetaInfPersistenceXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("persistence.xml", "", true)
                .build();
        checkLayersForArchive(p);
    }


    @Test
    public void testAnnotationFromRootPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaAnnotationFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }


    @Test
    public void testClassFromRootPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }


    @Test
    public void testClassFromCriteriaPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaClassFromCriteriaPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("jpa", "jpa"));
    }
}
