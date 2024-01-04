/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.naming;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class NamingLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    javax.naming.Context context;

    @Test
    public void testNamingClassUsage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(this.getClass())
                .build();
        // naming is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "naming");
    }

}
