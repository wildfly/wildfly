package org.wildfly.ee.feature.pack.layer.tests.jaxrs;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JaxrsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJaxrsAnnotationFromRootPackage() throws Exception {
        testOneClassInWar(JaxrsRootPackageAnnotationUsage.class);
    }

    @Test
    public void testJaxrsClassFromRootPackage() throws Exception {
        testOneClassInWar(JaxrsRootPackageClassUsage.class);
    }

    @Test
    public void testJaxrsAnnotationFromCorePackage() throws Exception {
        testOneClassInWar(JaxrsCorePackageAnnotationUsage.class);
    }

    @Test
    public void testJaxrsClassFromCorePackage() throws Exception {
        testOneClassInWar(JaxrsCorePackageClassUsage.class);
    }

    @Test
    public void testApplicationInWebXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml(
                        "web.xml",
                        createXmlElementWithContent("jakarta.ws.rs.core.Application", "web-app", "servlet", "servlet-class"))
                .build();
        checkLayersForArchive(p, "jaxrs", "servlet");
    }

    private void testOneClassInWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "jaxrs");
    }
}
