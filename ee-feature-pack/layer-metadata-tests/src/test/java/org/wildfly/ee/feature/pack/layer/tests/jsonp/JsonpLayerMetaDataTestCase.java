/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jsonp;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JsonpLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testClassFromRootPackageUsage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsonpClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jsonp");
    }

    @Test
    public void testClassFromStreamPackageUsage() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsonpClassFromStreamPackageUsage.class)
                .build();
        // jsonp is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "jsonp");
    }
}
