/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jsonb;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;
import java.util.Set;

public class JsonbLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationUsage() {
        testSingleClassWar(JsonbAnnotationUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() {
        testSingleClassWar(JsonbClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromAdapterPackageUsage() {
        testSingleClassWar(JsonbClassFromAdapterPackageUsage.class);
    }

    @Test
    public void testClassFromConfigPackageUsage() {
        testSingleClassWar(JsonbClassFromConfigPackageUsage.class);
    }

    @Test
    public void testClassFromSerializerPackageUsage() {
        testSingleClassWar(JsonbClassFromSerializerPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchiveDontContainJsonp(p);
    }

    private void checkLayersForArchiveDontContainJsonp(Path p) {
        // Some extra checks here, since jsonp has the jakarta.json package
        // while jsonb uses jakarta.json.bind. We want to make sure the rule
        // for jsonp is tight enough to not inadvertently recommending jsonb as well

        //jsonb is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        Set<String> set = checkLayersForArchive(p, "jsonb");
        Assert.assertFalse(set.contains("jsonp"));
    }
}
