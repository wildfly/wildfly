/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.cdi;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.JAR;
import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.WAR;

public class CdiLayerTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testCdiDetectedFileInWar() {
        Path p = createArchiveBuilder(WAR)
                .addXml("beans.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testCdiDetectedFileInJar() {
        Path p = createArchiveBuilder(JAR)
                .addXml("beans.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testInjectPackage() {
        Path p = createArchiveBuilder(WAR)
                .addClasses(CdiInjectClass.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testEnterpriseContext() {
        //This is in a sub-package of jakarta.enterprise.context
        Path p = createArchiveBuilder(WAR)
                .addClasses(CdiEnterpriseContextClass.class)
                .build();
        // cdi is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        // cdi is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "cdi");
    }

}
