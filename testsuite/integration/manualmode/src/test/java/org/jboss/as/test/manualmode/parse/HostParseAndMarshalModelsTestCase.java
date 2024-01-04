/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.parse;

import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests the host configuration files can be parsed and marshalled.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Parameterized.class)
public class HostParseAndMarshalModelsTestCase extends AbstractParseAndMarshalModelsTestCase {
    private static final Logger LOGGER = Logger.getLogger(HostParseAndMarshalModelsTestCase.class);

    @Parameterized.Parameters
    public static List<Path> data() {
        return resolveConfigFiles(p -> p.getFileName()
                .toString()
                // Cannot test the host-secondary.xml as the system property used for testing cannot be written
                .startsWith("host") && !"host-secondary.xml".equals(p.getFileName()
                .toString()), "domain", "configuration");
    }

    @Parameterized.Parameter
    public Path configFile;

    @Test
    public void configFiles() throws Exception {
        LOGGER.infof("Testing config file %s", configFile);
        hostXmlTest(configFile.toFile());
    }
}
