package org.wildfly.feature.pack.layer.tests.microprofile.reactive.streams.operators;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveStreamsOperatorsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testClassFromRootPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(ReactiveStreamsOperatorsClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "microprofile-reactive-streams-operators");
    }

    @Test
    public void testClassFromSpiPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(ReactiveStreamsOperatorsClassFromSpiPackageUsage.class)
                .build();
        checkLayersForArchive(p, "microprofile-reactive-streams-operators");
    }
}
