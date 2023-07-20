package org.wildfly.ee.feature.pack.layer.tests.webservices;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class WebServicesLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationInRootJwsPackage() throws Exception {
        testSingleWar(AnnotationInRootJwsPackageUsage.class);
    }

    @Test
    public void testAnnotationInJwsSoapPackage() throws Exception {
        testSingleWar(AnnotationInJwsSoapPackageUsage.class);
    }

    @Test
    public void testAnnotationInRootXmlPackage() throws Exception {
        testSingleWar(AnnotationInRootXmlPackageUsage.class);
    }

    @Test
    public void testAnnotationInXmlChildPackage() throws Exception {
        testSingleWar(AnnotationInXmlChildPackageUsage.class);
    }

    @Test
    public void testClassInRootXmlPackage() throws Exception {
        testSingleWar(ClassInRootXmlPackageUsage.class);
    }

    @Test
    public void testClassInXmlHandlerSoapPackage() throws Exception {
        testSingleWar(ClassInXmlHandlerSoapPackageUsage.class);
    }

    @Test
    public void testClassInXmlHandlerPackage() throws Exception {
        testSingleWar(ClassInXmlHandlerPackageUsage.class);
    }

    @Test
    public void testClassInXmlHttpPackage() throws Exception {
        testSingleWar(ClassInXmlHttpPackageUsage.class);
    }

    @Test
    public void testClassInXmlSoapPackage() throws Exception {
        testSingleWar(ClassInXmlSoapPackageUsage.class);
    }

    @Test
    public void testClassInXmlWsaddressingPackage() throws Exception {
        testSingleWar(ClassInXmlWsaddressingPackageUsage.class);
    }

    private void testSingleWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "webservices");
    }
}
