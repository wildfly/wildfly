package org.wildfly.ee.feature.pack.layer.tests.web.passivation;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class WebPassivationLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testDistributableInWebXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("web.xml", createXmlElementWithContent("", "web-app", "distributable"))
                .build();
        checkLayersForArchive(p, "web-passivation");
    }
}
