/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.pojo;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class PojoLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJarMetaInfBeansXml() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("one-jboss-beans.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testWarWebInfBeansXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("xjboss-beans.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testWarWebInfClassesMetaInfBeansXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("jboss-beans.xml", "", true)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("pojo", "pojo"));
    }
}
