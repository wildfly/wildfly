/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.B_OVERRIDES_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.FROM_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.FROM_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.NOT_AVAILABLE_NESTED_DIR_UNDER_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.NOT_AVAILABLE_NESTED_DIR_UNDER_B;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.junit.Assert;

/**
 * Add a config-source with a custom class in the microprofile-config subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SetupTask extends CLIServerSetupTask {
    private static final String PROPS_A = "propsA";
    private static final String PROPS_B = "propsB";

    private static final String ADDR_A = "/subsystem=microprofile-config-smallrye/config-source=propsA";
    private static final String ADDR_B = "/subsystem=microprofile-config-smallrye/config-source=propsB";
    private static final String ADDR_NON_EXISTENT = "/subsystem=microprofile-config-smallrye/config-source=not-there";

    static final String A = "val-a";
    static final String B = "val-b";
    // In propsA this will be 'overridden-a', in propsB 'overridden-b'. Due to the relative ordinals of the config sources, propsB should win
    private static final String OVERRIDDEN_A = "overridden-a";
    static final String OVERRIDDEN_B = "overridden-b";

    private volatile Path rootDir;
    private volatile Path nonExistent;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        Path target = Paths.get("target").toAbsolutePath().normalize();
        rootDir = Files.createTempDirectory(target, "test");
        Assert.assertTrue(Files.exists(rootDir));

        nonExistent = Files.createTempDirectory(target, "duff");
        deleteDirectory(nonExistent);
        Assert.assertFalse(Files.exists(nonExistent));

        Path dirA = createPropsDir(rootDir, PROPS_A, FROM_A, A, B_OVERRIDES_A, OVERRIDDEN_A);
        Path dirB = createPropsDir(rootDir, PROPS_B, FROM_B, B, B_OVERRIDES_A, OVERRIDDEN_B);

        // Add some files which will be ignored (since the config sources we are adding here are not roots)
        // in child directories of the config source directories
        createPropsDir(dirA, PROPS_A, NOT_AVAILABLE_NESTED_DIR_UNDER_A, "Hello");
        createPropsDir(dirB, PROPS_B, NOT_AVAILABLE_NESTED_DIR_UNDER_B, "Hello");


        NodeBuilder nb = builder.node(containerId);


        nb.setup(String.format("%s:add(dir={path=\"%s\"})", ADDR_A, escapePath(dirA)));
        nb.setup(String.format("/path=mp-config-test:add(path=\"%s\")", escapePath(dirB.getParent())));
        // Make this one explicitly set root=false for test coverage
        nb.setup(String.format("%s:add(dir={relative-to=mp-config-test, path=\"%s\", root=false}, ordinal=300)", ADDR_B, dirB.getFileName()));
        nb.setup(String.format("%s:add(dir={path=\"%s\"})", ADDR_NON_EXISTENT, escapePath(nonExistent)));

        nb.teardown(String.format("%s:remove", ADDR_A));
        nb.teardown(String.format("%s:remove", ADDR_B));
        nb.teardown(String.format("%s:remove", ADDR_NON_EXISTENT));
        nb.teardown("/path=mp-config-test:remove");

        super.setup(managementClient, containerId);
    }

    private String escapePath(Path path) {
        String s = path.toString();
        //Avoid problems with paths on Windows
        s = s.replace('\\', '/');
        return s;
    }

    private Path createPropsDir(Path rootDir, String sourceName, String... props) throws IOException {
        Path sourceDir = rootDir.resolve(sourceName);
        Files.createDirectory(sourceDir);
        Assert.assertTrue(Files.exists(sourceDir));

        for (int i = 0 ; i < props.length ; i += 2) {
            Path file = sourceDir.resolve(props[i]);
            Files.createFile(file);
            Assert.assertTrue(Files.exists(file));
            Files.write(file, Collections.singletonList(props[i + 1]));
        }
        return sourceDir.toAbsolutePath().normalize();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        super.tearDown(managementClient, containerId);
        deleteDirectory();
    }

    private void deleteDirectory() throws IOException {
        deleteDirectory(rootDir);
    }

    private void deleteDirectory(Path rootDir) throws IOException {
        if (rootDir != null && Files.exists(rootDir)) {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
        }
    }
}
