package org.wildfly.feature.pack.layer.tests.microprofile.lra;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileLraLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testClassFromRootPackage() throws Exception {
        testSingleClassWar(MicroProfileLraClassFromRootPackageUsage.class);
    }


    @Test
    public void testAnnotationFromAnnotationPackage() throws Exception {
        testSingleClassWar(MicroProfileLraAnnotationFromAnnotationPackageUsage.class);
    }

    @Test
    public void testClassFromAnnotationPackage() throws Exception {
        testSingleClassWar(MicroProfileLraClassFromAnnotationPackageUsage.class);
    }

    @Test
    public void testAnnotationFromAnnotationWsRsPackage() throws Exception {
        testSingleClassWar(MicroProfileLraAnnotationFromAnnotationWsRsPackageUsage.class);
    }


    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(AbstractLayerMetaDataTestCase.ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-lra-participant");
    }
}
