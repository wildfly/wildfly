/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jpa.distributed;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;
import org.wildfly.ee.feature.pack.layer.tests.jpa.JpaClassFromCriteriaPackageUsage;

import java.nio.file.Path;
import java.util.Collections;

public class JpaDistributedHaProfileMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testClassFromCriteriaPackage() {
        // The test in the parent package covers the standard profile
        Path p = createArchiveBuilder(AbstractLayerMetaDataTestCase.ArchiveType.WAR)
                .addClasses(JpaClassFromCriteriaPackageUsage.class)
                .build();
        checkLayersForArchive(p,
                builder -> builder.setExecutionProfiles(Collections.singleton("ha")),
                new AbstractLayerMetaDataTestCase.ExpectedLayers("jpa", "jpa-distributed")
                        .excludedLayers("jpa"));
    }

}
