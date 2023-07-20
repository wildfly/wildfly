package org.wildfly.ee.feature.pack.layer.tests.ejb.lite;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EjbLiteLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testEjbLiteAnnotationUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(EjbLiteAnnotationUsage.class)
                .build();
        checkLayersForArchive(p,"ejb-lite");
    }

    @Test
    public void testEjbLiteClassUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(EjbLiteClassUsage.class)
                .build();
        checkLayersForArchive(p,"ejb-lite");
    }

}
