/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.reactive.streams.operators;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveStreamsOperatorsLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testClassFromRootPackage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(ReactiveStreamsOperatorsClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassFromSpiPackage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(ReactiveStreamsOperatorsClassFromSpiPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("microprofile-reactive-streams-operators", "microprofile-reactive-streams-operators"));
    }
}
