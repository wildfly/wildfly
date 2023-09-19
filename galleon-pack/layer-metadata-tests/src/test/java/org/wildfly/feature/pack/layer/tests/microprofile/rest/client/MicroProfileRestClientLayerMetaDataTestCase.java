package org.wildfly.feature.pack.layer.tests.microprofile.rest.client;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileRestClientLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testClassFromRootPackage() throws Exception {
        testSingleClassWar(MicroProfileRestClientClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromExtPackage() throws Exception {
        testSingleClassWar(MicroProfileRestClientClassFromExtPackageUsage.class);
    }

    @Test
    public void testClassFromSpiPackage() throws Exception {
        testSingleClassWar(MicroProfileRestClientClassFromSpiPackageUsage.class);
    }

    @Test
    public void testAnnotationFromAnnotationsPackage() throws Exception {
        testSingleClassWar(MicroProfileRestClientAnnotationFromAnnotationsPackageUsage.class);
    }

    @Test
    public void testAnnotationFromInjectPackage() throws Exception {
        testSingleClassWar(MicroProfileRestClientAnnotationFromInjectPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-rest-client");
    }
}
