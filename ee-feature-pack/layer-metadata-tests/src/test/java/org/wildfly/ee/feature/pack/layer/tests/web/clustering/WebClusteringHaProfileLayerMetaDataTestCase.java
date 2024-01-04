/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.web.clustering;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;
import org.wildfly.glow.ScanArguments;

import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Consumer;

public class WebClusteringHaProfileLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testDistributableInWebXml() {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", createXmlElementWithContent("", "web-app", "distributable"))
                .build();
        checkLayersForArchive(p,
                new Consumer<ScanArguments.Builder>() {
                    @Override
                    public void accept(ScanArguments.Builder builder) {
                        builder.setExecutionProfiles(Collections.singleton("ha"));
                    }
                },
                new ExpectedLayers("web-passivation", "web-clustering")
                        .excludedLayers("web-passivation"));
    }
}
