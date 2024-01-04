/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.reactive.messaging;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveMessagingLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationFromRootPackage() {
        testSingleClassWar(MicroProfileReactiveMessagingAnnotationFromRootPackage.class);
    }

    @Test
    public void testClassFromRootPackage() {
        testSingleClassWar(MicroProfileReactiveMessagingClassFromRootPackage.class);
    }

    @Test
    public void testAnnotationFromSpiPackage() {
        testSingleClassWar(MicroProfileReactiveMessagingAnnotationFromSpiPackage.class);
    }

    @Test
    public void testClassFromSpiPackage() {
        testSingleClassWar(MicroProfileReactiveMessagingClassFromSpiPackage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("microprofile-reactive-messaging", "microprofile-reactive-messaging"));
    }
}
