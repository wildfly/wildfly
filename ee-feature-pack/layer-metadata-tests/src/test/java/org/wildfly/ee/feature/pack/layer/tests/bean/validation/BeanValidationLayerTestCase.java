package org.wildfly.ee.feature.pack.layer.tests.bean.validation;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

public class BeanValidationLayerTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testBeanValidationAnnotationInRootPackageUsage() throws Exception {
        testClass(BeanValidationAnnotationInRootPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInConstraintValidationPackageUsage() throws Exception {
        testClass(BeanValidationAnnotationInConstraintValidationPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInConstraintsPackageUsage() throws Exception {
        testClass(BeanValidationAnnotationInConstraintsPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInGroupsPackageUsage() throws Exception {
        testClass(BeanValidationAnnotationInGroupsPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotationInValueExtractionPackageUsage() throws Exception {
        testClass(BeanValidationAnnotationInValueExtractionPackageUsage.class);
    }

    @Test
    public void testBeanValidationAnnotatonInExecutablePackageUsage() throws Exception {
        testClass(BeanValidationAnnotatonInExecutablePackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInRootPackageUsage() throws Exception {
        testClass(BeanValidationClassInRootPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInBootstrapPackageUsage() throws Exception {
        testClass(BeanValidationClassInBootstrapPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInConstraintValidationPackageUsage() throws Exception {
        testClass(BeanValidationClassInConstraintValidationPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInExecutablePackageUsage() throws Exception {
        testClass(BeanValidationClassInExecutablePackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInGroupsPackageUsage() throws Exception {
        testClass(BeanValidationClassInGroupsPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInMetadataPackageUsage() throws Exception {
        testClass(BeanValidationClassInMetadataPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInSpiPackageUsage() throws Exception {
        testClass(BeanValidationClassInSpiPackageUsage.class);
    }

    @Test
    public void testBeanValidationClassInValueExtractionPackageUsage() throws Exception {
        testClass(BeanValidationClassInValueExtractionPackageUsage.class);
    }

    private void testClass(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "bean-validation");
    }
}
