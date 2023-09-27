/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.bean.validation;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

public class BeanValidationLayerTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testBeanValidationAnnotationInRootPackageUsage() {
        testClass(BeanValidationAnnotationInRootPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInConstraintValidationPackageUsage() {
        testClass(BeanValidationAnnotationInConstraintValidationPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInConstraintsPackageUsage() {
        testClass(BeanValidationAnnotationInConstraintsPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInGroupsPackageUsage() {
        testClass(BeanValidationAnnotationInGroupsPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInValueExtractionPackageUsage() {
        testClass(BeanValidationAnnotationInValueExtractionPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotatonInExecutablePackageUsage() {
        testClass(BeanValidationAnnotatonInExecutablePackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInRootPackageUsage() {
        testClass(BeanValidationClassInRootPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInBootstrapPackageUsage() {
        testClass(BeanValidationClassInBootstrapPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInConstraintValidationPackageUsage() {
        testClass(BeanValidationClassInConstraintValidationPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInExecutablePackageUsage() {
        testClass(BeanValidationClassInExecutablePackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInGroupsPackageUsage() {
        testClass(BeanValidationClassInGroupsPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInMetadataPackageUsage() {
        testClass(BeanValidationClassInMetadataPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInSpiPackageUsage() {
        testClass(BeanValidationClassInSpiPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInValueExtractionPackageUsage() {
        testClass(BeanValidationClassInValueExtractionPackageUsage.class);
    }

    private void testClass(Class<?> clazz) {
        Path p = createArchiveBuilder(WAR)
                .addClasses(clazz)
                .build();
        // bean-validation is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "bean-validation");
    }
}
