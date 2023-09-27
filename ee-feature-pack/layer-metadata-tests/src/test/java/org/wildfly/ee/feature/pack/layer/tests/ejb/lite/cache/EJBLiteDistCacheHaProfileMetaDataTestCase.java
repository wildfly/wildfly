/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ejb.lite.cache;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;
import org.wildfly.ee.feature.pack.layer.tests.ejb.lite.EjbLiteAnnotationUsage;

import java.nio.file.Path;
import java.util.Collections;

public class EJBLiteDistCacheHaProfileMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationHaProfile() {
        // The tests in the parent package test standard profile
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(EjbLiteAnnotationUsage.class)
                .build();
        checkLayersForArchive(p,
                builder -> builder.setExecutionProfiles(Collections.singleton("ha")),
                new ExpectedLayers("ejb-lite", "ejb-lite")
                        .addDecorator("ejb-dist-cache")
                        .excludedLayers("ejb-local-cache"));
    }
}
