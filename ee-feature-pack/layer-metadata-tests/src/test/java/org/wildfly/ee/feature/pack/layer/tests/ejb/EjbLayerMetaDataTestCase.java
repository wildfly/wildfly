/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ejb;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EjbLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testMessageDrivenAnnotation() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MessageDrivenAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testRemoteAnnotation() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(RemoteAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testMessageDrivenContextClass() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MessageDrivenContextClassUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    public void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("ejb", "ejb"));
    }
}
