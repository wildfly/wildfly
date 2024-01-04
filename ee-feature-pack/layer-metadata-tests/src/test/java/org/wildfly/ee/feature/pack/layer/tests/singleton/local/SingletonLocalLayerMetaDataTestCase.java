/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.singleton.local;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class SingletonLocalLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testSingletonDeploymentXmlInMetaInf() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("singleton-deployment.xml", "")
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testSingletonDeploymentXmlInWebInfClassesMetaInf() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("singleton-deployment.xml", "", true)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testSingletonDeploymentClassUsageFromRootPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testSingletonDeploymentClassUsageFromElectionPackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromElectionPackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testSingletonDeploymentClassUsageFromServicePackage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromServicePackageUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("singleton-local", "singleton-local"));
    }
}
