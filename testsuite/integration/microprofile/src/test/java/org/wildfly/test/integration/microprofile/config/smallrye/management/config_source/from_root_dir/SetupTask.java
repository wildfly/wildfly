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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.NOT_AVAILABLE_NESTED_DIR_UNDER_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.NOT_AVAILABLE_ROOT_FILE;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.Y_A_OVERRIDES_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.X_D_OVERRIDES_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.FROM_A1;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.FROM_A2;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.FROM_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir.TestApplication.Z_C_OVERRIDES_A;

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
 * Adds config-source-roots in the microprofile-config subsystem.
 *
 * @author Kabir Khan
 */
public class SetupTask extends CLIServerSetupTask {
    private static final String PROPS_A = "propsA";
    private static final String PROPS_B = "propsB";
    private static final String PROPS_C = "propsC";
    private static final String PROPS_D = "propsD";

    private static final String ROOT_A = "/subsystem=microprofile-config-smallrye/config-source=rootA";
    private static final String ROOT_B = "/subsystem=microprofile-config-smallrye/config-source=rootB";
    private static final String ROOT_NON_EXISTENT = "/subsystem=microprofile-config-smallrye/config-source=bad-root";

    static final String A1 = "val-a1";
    static final String A2 = "val-a2";
    static final String B = "val-b";
    static final String X_FROM_A = "x-from-a";
    static final String X_FROM_D = "x-from-d";
    static final String Y_FROM_A = "y-from-a";
    static final String Y_FROM_B = "y-from-b";
    static final String Z_FROM_A = "z-from-a";
    static final String Z_FROM_C = "z-from-c";

    private volatile Path rootDir1;
    private volatile Path rootDir2;
    private volatile Path nonExistent;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        Path target = Paths.get("target").toAbsolutePath().normalize();
        rootDir1 = Files.createTempDirectory(target, "test1");
        rootDir2 = Files.createTempDirectory(target, "test2");
        Assert.assertTrue(Files.exists(rootDir1));
        Assert.assertTrue(Files.exists(rootDir2));

        nonExistent = Files.createTempDirectory(target, "duff");
        deleteDirectory(nonExistent);
        Assert.assertFalse(Files.exists(nonExistent));

        // Since PROPS_A is alphabetically lower than PROPS_B, Y will come from PROPS_A
        Path dirA = createPropsDir(rootDir1, PROPS_A, FROM_A1, A1, FROM_A2, A2, X_D_OVERRIDES_A, X_FROM_A, Y_A_OVERRIDES_B, Y_FROM_A, Z_C_OVERRIDES_A, Z_FROM_A);
        createPropsDir(rootDir1, PROPS_B, FROM_B, B, Y_A_OVERRIDES_B, Y_FROM_B);
        // Since PROPS_C has a higher ordinal, Z will come from PROPS_A
        createPropsDir(rootDir1, PROPS_C, "config_ordinal", "500", Z_C_OVERRIDES_A, Z_FROM_C);

        // Since this config-source-root has a higher ordinal than rootDir1, X will be used from here
        createPropsDir(rootDir2, PROPS_D, X_D_OVERRIDES_A, X_FROM_D);

        NodeBuilder nb = builder.node(containerId);

        //Create some files in locations that should not be considered as properties for a config source root director
        // 1) in the root directory itself
        Files.write(rootDir1.resolve(NOT_AVAILABLE_ROOT_FILE), Collections.singletonList("Hello"));
        // 2) in a nested folder under one of the folders under the root dirctory
        createPropsDir(dirA, PROPS_A, NOT_AVAILABLE_NESTED_DIR_UNDER_A, "Hello");


        nb.setup(String.format("%s:add(dir={root=true, path=\"%s\"})", ROOT_A, escapePath(rootDir1)));
        nb.setup(String.format("/path=mp-config-test:add(path=\"%s\")", escapePath(rootDir2.getParent())));
        nb.setup(String.format("%s:add(dir={root=true, relative-to=mp-config-test, path=\"%s\"}, ordinal=300)", ROOT_B, rootDir2.getFileName()));
        nb.setup(String.format("%s:add(dir={root=true, path=\"%s\"})", ROOT_NON_EXISTENT, escapePath(nonExistent)));

        nb.teardown(String.format("%s:remove", ROOT_A));
        nb.teardown(String.format("%s:remove", ROOT_B));
        nb.teardown(String.format("%s:remove", ROOT_NON_EXISTENT));
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
        deleteDirectory(rootDir1);
        deleteDirectory(rootDir2);
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
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
