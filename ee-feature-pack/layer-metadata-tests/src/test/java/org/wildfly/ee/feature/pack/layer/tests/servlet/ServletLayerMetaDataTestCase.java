package org.wildfly.ee.feature.pack.layer.tests.servlet;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ServletLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {



    @Test
    public void testServletAnnotation() throws Exception {
        // No sub packages for this one
        testSingleClassWar(ServletAnnotationUsage.class);
    }

    @Test
    public void testWebsocketAnnotationFromRootPackage() throws Exception {
        testSingleClassWar(WebsocketAnnotationFromRootPackageUsage.class);
    }

    @Test
    public void testWebsocketAnnotationFromServerPackage() throws Exception {
        testSingleClassWar(WebsocketAnnotationFromServerPackageUsage.class);
    }

    @Test
    public void testServletInWebXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", createXmlElementWithContent("", "web-app", "servlet"))
                .build();
        checkLayersForArchive(p, "servlet");
    }

    @Test
    public void testServletClassFromRootPackage() throws Exception {
        testSingleClassWar(ServletClassFromRootPackageUsage.class);
    }

    @Test
    public void testServletClassFromDescriptorPackage() throws Exception {
        testSingleClassWar(ServletClassFromDescriptorPackageUsage.class);
    }

    @Test
    public void testServletClassFromHttpPackage() throws Exception {
        testSingleClassWar(ServletClassFromDescriptorPackageUsage.class);
    }

    @Test
    public void testWebsocketClassFromRootPackage() throws Exception {
        testSingleClassWar(WebSocketClassFromRootPackageUsage.class);
    }

    @Test
    public void testWebsocketClassFromServerPackage() throws Exception {
        testSingleClassWar(WebSocketClassFromServerPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "servlet");
    }
}
