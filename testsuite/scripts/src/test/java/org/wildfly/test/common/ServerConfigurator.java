/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerConfigurator {
    public static final Set<Path> PATHS = new LinkedHashSet<>(16);
    private static final AtomicBoolean CONFIGURED = new AtomicBoolean(false);

    public static void configure() throws IOException, InterruptedException {
        if (CONFIGURED.compareAndSet(false, true)) {
            // Always add the default path
            PATHS.add(ServerHelper.JBOSS_HOME);

            final String serverName = System.getProperty("server.output.dir.prefix");

            // Create special characters in paths to test with assuming the -Dserver.name was not used
            if (serverName == null || serverName.isEmpty()) {
                PATHS.add(copy("wildfly core"));
                PATHS.add(copy("wildfly (core)"));
            }
        }
    }

    private static Path copy(final String targetName) {
        final Path source = ServerHelper.JBOSS_HOME;
        try {
            final Path target = source.getParent().resolve(targetName);
            deleteDirectory(target);
            copyDirectory(source, target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyDirectory(final Path source, final Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                final Path targetDir = target.resolve(source.relativize(dir));
                Files.copy(dir, targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(final Path dir) throws IOException {
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
