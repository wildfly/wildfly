/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.parse;

import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Parameterized.class)
public class StandaloneLegacyParseAndMarshalModelsTestCase extends AbstractParseAndMarshalModelsTestCase {
    private static final Logger LOGGER = Logger.getLogger(StandaloneParseAndMarshalModelsTestCase.class);

    @Parameterized.Parameters
    public static List<Path> data() {
        return resolveLegacyConfigFiles("standalone");
    }

    @Parameterized.Parameter
    public Path configFile;

    @Test
    public void configFiles() throws Exception {
        LOGGER.infof("Testing config file %s", configFile);
        Assume.assumeFalse(altDistTest);
        standaloneXmlTest(configFile.toFile());
    }
}
