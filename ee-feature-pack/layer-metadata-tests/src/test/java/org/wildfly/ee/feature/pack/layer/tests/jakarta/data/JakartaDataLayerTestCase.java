/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ee.feature.pack.layer.tests.jakarta.data;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

import java.nio.file.Path;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

public class JakartaDataLayerTestCase extends AbstractLayerMetaDataTestCase {

   @Test
    public void testJakartaDataAnnotationInMetamodelPackageUsage() {
        testClass(JakartaDataAnnotationInMetamodelPackageUsage.class);
    }

    @Test
    public void testJakartaDataAnnotationInRepositoryPackageUsage() {
        testClass(JakartaDataAnnotationInRepositoryPackageUsage.class);
    }

    @Test
    public void testJakartaDataAnnotationInSpiPackageUsage() {
        testClass(JakartaDataAnnotationInSpiPackageUsage.class);
    }

    @Test
    public void testJakartaDataClassInRootPackageUsage() {
        testClass(JakartaDataClassInRootPackageUsage.class);
    }

    @Test
    public void testJakartaDataClassInExceptionsPackageUsage() {
        testClass(JakartaDataClassInExceptionsPackageUsage.class);
    }

    @Test
    public void testJakartaDataClassInMetamodelPackageUsage() {
        testClass(JakartaDataClassInMetamodelPackageUsage.class);
    }

    @Test
    public void testJakartaDataClassInPagePackageUsage() {
        testClass(JakartaDataClassInPagePackageUsage.class);
    }

    @Test
    public void testJakartaDataClassInRepositoryPackageUsage() {
        testClass(JakartaDataClassInRepositoryPackageUsage.class);
    }

    private void testClass(Class<?> clazz) {
        Path p = createArchiveBuilder(WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("jakarta-data", "jakarta-data"));
    }
}
