/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.manualmode.parse;

import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests the example configuration files can be parsed and marshalled.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Parameterized.class)
public class ExampleParseAndMarshalModelsTestCase extends AbstractParseAndMarshalModelsTestCase {
    private static final Logger LOGGER = Logger.getLogger(ExampleParseAndMarshalModelsTestCase.class);

    @Parameterized.Parameters
    public static List<Path> data() {
        return resolveConfigFiles(p -> p.getFileName().toString().startsWith("standalone"), "docs", "examples", "configs");
    }

    @Parameterized.Parameter
    public Path configFile;

    @Test
    public void configFiles() throws Exception {
        LOGGER.infof("Testing config file %s", configFile);
        standaloneXmlTest(configFile.toFile());
    }
}
