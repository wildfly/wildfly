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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.test.shared.ModelParserUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Abstract test case for testing the ability to parse configuration files. As well as the ability to marshal them back
 * to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
abstract class AbstractParseAndMarshalModelsTestCase {

    private static final File JBOSS_HOME = Paths.get("target", "jbossas-parse-marshal").toFile();

    protected ModelNode standaloneXmlTest(File original) throws Exception {
        return ModelParserUtils.standaloneXmlTest(original, JBOSS_HOME);
    }

    protected void hostXmlTest(final File original) throws Exception {
        ModelParserUtils.hostXmlTest(original, JBOSS_HOME);
    }

    protected ModelNode domainXmlTest(final File original) throws Exception {
        return ModelParserUtils.domainXmlTest(original, JBOSS_HOME);
    }

    //  Get-config methods

    static List<Path> resolveConfigFiles(final String... names) {
        return resolveConfigFiles(p -> p.getFileName().toString().endsWith(".xml"), names);
    }

    static List<Path> resolveConfigFiles(final Predicate<Path> filter, final String... names) {
        final String path = System.getProperty("jboss.dist");
        Assert.assertNotNull("jboss.dist property was not set", path);
        final Path configDir = Path.of(path, names);
        Assert.assertTrue(String.format("Directory %s does not exist.", path), Files.exists(configDir));
        try (Stream<Path> files = Files.list(configDir)) {
            return files.filter(filter)
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
