/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ejb.lite;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EjbLiteLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testEjbLiteAnnotationUsage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(EjbLiteAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testEjbLiteClassUsage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(EjbLiteClassUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p,new ExpectedLayers("ejb-lite", "ejb-lite"));
    }
}
