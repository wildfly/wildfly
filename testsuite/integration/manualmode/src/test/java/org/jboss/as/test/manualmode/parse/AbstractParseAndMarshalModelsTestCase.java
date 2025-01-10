/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    static final boolean altDistTest = System.getProperty("build.output.dir", "").startsWith("ee-");

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
    static List<Path> resolveLegacyConfigFiles(final String... names) {
        return resolveLegacyConfigFiles(p -> p.getFileName().toString().endsWith(".xml"), names);
    }

    static List<Path> resolveLegacyConfigFiles(final Predicate<Path> filter, final String... names) {
        final String subDir = System.getProperty("jbossas.ts.submodule.dir");
        Assert.assertNotNull("\"jbossas.ts.submodule.dir\" property was not set", subDir);
        final Path path = Path.of(subDir, "src/test/resources/legacy-configs/").toAbsolutePath();
        final Path configDir = Path.of(path.toString(), names);
        Assert.assertTrue(String.format("Directory %s does not exist.", path), Files.exists(configDir));
        try (Stream<Path> files = Files.list(configDir)) {
            return files.filter(filter)
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
