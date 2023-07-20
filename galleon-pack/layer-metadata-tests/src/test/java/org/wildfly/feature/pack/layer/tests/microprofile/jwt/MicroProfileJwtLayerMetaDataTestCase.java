package org.wildfly.feature.pack.layer.tests.microprofile.jwt;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileJwtLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAuthPackageAnnotation() throws Exception {
        testSingleClassWar(MicroProfileJwtAuthPackageAnnotation.class);
    }

    @Test
    public void testJwtPackageAnnotation() throws Exception {
        testSingleClassWar(MicroProfileJwtJwtPackageAnnotation.class);
    }

    @Test
    public void testJwtPackageClass() throws Exception {
        testSingleClassWar(MicroProfileJwtJwtPackageClass.class);
    }

    @Test
    public void testJwtConfigPackageClass() throws Exception {
        testSingleClassWar(MicroProfileJwtJwtConfigPackageAnnotation.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-jwt");
    }
}
