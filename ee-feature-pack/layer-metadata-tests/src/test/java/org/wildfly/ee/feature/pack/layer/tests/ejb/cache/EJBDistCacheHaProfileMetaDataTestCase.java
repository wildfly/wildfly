/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ejb.cache;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;
import org.wildfly.ee.feature.pack.layer.tests.ejb.MessageDrivenAnnotationUsage;

import java.nio.file.Path;
import java.util.Collections;

public class EJBDistCacheHaProfileMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testMessageDrivenAnnotationHaProfile() {
        // The tests in the parent package test standard profile
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MessageDrivenAnnotationUsage.class)
                .build();
        checkLayersForArchive(p,
                builder -> builder.setExecutionProfiles(Collections.singleton("ha")),
                new ExpectedLayers("ejb", "ejb")
                        .addDecorator("ejb-dist-cache")
                        .excludedLayers("ejb-local-cache"));
    }
}
