package org.wildfly.feature.pack.layer.tests.microprofile.fault.tolerance;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileFaultToleranceLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationUsage() throws Exception {
        // Only package containing annotations
        testSingleClassWar(MicroProfileFaultToleranceAnnotationUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() throws Exception {
        testSingleClassWar(MicroProfileFaultToleranceClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromExceptionsPackageUsage() throws Exception {
        testSingleClassWar(MicroProfileFaultToleranceClassFromExceptionsPackageUsage.class);
    }


    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "microprofile-fault-tolerance");
    }
}
