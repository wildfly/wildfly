/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.singleton.ha;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;
import java.util.Collections;

public class SingletonHaHaProfileLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testSingletonDeploymentXmlInMetaInf() {
        // The tests in ../local test the standard profile
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("singleton-deployment.xml", "")
                .build();

        checkLayersForArchive(p,
                builder -> builder.setExecutionProfiles(Collections.singleton("ha")),
                new ExpectedLayers("singleton-local", "singleton-ha")
                        .excludedLayers("singleton-local"));
    }
}
