/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.messaging.activemq;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MessagingActiveMqLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationUsage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(MessagingActiveMqAnnotationUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    @Test
    public void testClassUsage() {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(MessagingActiveMqClassUsage.class)
                .build();
        checkLayersForArchive(p);
    }

    private void checkLayersForArchive(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("messaging-activemq", "messaging-activemq"));
    }
}
