package org.wildfly.ee.feature.pack.layer.tests.ee.integration;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeIntegrationLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJakartaAnnotationSecurityPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationSecurityPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaAnnotationSqlPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationSqlPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaAnnotationPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaXmlBindAnnotationAdaptersPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaXmlBindAnnotationAdaptersPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaXmlBindAnnotationPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaXmlBindAnnotationPackageAnnotationUsage.class);
    }


    @Test
    public void testJakartaXmlBindRootPackageClassUsage() throws Exception {
        testWarWithClass(JakartaXmlBindRootPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlAnnotationPackageClassUsage() throws Exception {
        testWarWithClass(JakartaXmlAnnotationPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlAnnotationAdaptersPackageClassUsage() throws Exception {
        testWarWithClass(JakartaXmlAnnotationAdaptersPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindAttachmentPackageClassUsage() throws Exception {
        testWarWithClass(JakartaXmlBindAttachmentPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindHelpersPackageClassUsage() throws Exception {
        // jakarta.xml.bind.helpers.AbstractMarshallerImpl
        testWarWithClass(JakartaXmlBindHelpersPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindUtilPackageClassUsage() throws Exception {
        testWarWithClass(JakartaXmlBindUtilPackageClassUsage.class);
    }

    private void testWarWithClass(Class<?> clazz) throws Exception {
        Path p= createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "ee-integration");
    }
}
