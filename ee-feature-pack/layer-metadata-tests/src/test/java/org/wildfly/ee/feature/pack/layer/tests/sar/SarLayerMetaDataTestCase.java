/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.sar;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class SarLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testMetaInfJbossServiceXml() {
        Path p = createArchiveBuilder(ArchiveType.SAR)
                .addXml("jboss-service.xml", "")
                .build();
        checkLayersForArchive(p, new ExpectedLayers("sar", "sar"));
    }
}
