package org.wildfly.ee.feature.pack.layer.tests.elytron.oidc.client;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class ElytronOidcClientLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testXmlAuthMethod() throws Exception {
        String xml = createXmlElementWithContent("OIDC", "web-app", "login-config", "auth-method");
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", xml)
                .build();
        checkLayersForArchive(p, "elytron-oidc-client");
    }
}
