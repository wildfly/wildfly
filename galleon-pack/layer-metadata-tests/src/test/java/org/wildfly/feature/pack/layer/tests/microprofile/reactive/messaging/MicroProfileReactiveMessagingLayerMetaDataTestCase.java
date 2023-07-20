package org.wildfly.feature.pack.layer.tests.microprofile.reactive.messaging;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveMessagingLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationFromRootPackage() throws Exception {
        testSingleClassWar(MicroProfileReactiveMessagingAnnotationFromRootPackage.class);
    }

    @Test
    public void testClassFromRootPackage() throws Exception {
        testSingleClassWar(MicroProfileReactiveMessagingClassFromRootPackage.class);
    }

    @Test
    public void testAnnotationFromSpiPackage() throws Exception {
        testSingleClassWar(MicroProfileReactiveMessagingAnnotationFromSpiPackage.class);
    }

    @Test
    public void testClassFromSpiPackage() throws Exception {
        testSingleClassWar(MicroProfileReactiveMessagingClassFromSpiPackage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-reactive-messaging");
    }
}
