/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.elytron.oidc.client;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ElytronOidcClientLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testXmlAuthMethod() {
        String xml = createXmlElementWithContent("OIDC", "web-app", "login-config", "auth-method");
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", xml)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("elytron-oidc-client", "elytron-oidc-client"));
    }
}
