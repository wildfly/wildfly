package org.wildfly.feature.pack.layer.tests.microprofile.health;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileHealthLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testMicroProfileHealthAnnotationUsage() throws Exception {
        // There is only one package with annotations
        testSingleClassWar(MicroProfileHealthAnnotationUsage.class);
    }

    @Test
    public void testMicroProfileHealthClassUsage() throws Exception {
        // There is only one package with classes (the .spi child package is for implementors only)
        // There is only one package with annotations
        testSingleClassWar(MicroProfileHealthClassUsage.class);
    }


    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(AbstractLayerMetaDataTestCase.ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-health");
    }
}
