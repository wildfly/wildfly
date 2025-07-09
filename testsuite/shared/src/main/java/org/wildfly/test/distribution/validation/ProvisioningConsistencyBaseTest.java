/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.distribution.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.FileUtils;
import static org.jboss.as.test.shared.FileUtils.computeHash;
import static org.jboss.as.test.shared.FileUtils.unzipFile;

import org.jboss.logging.Logger;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Validates that provisioning using wildfly-maven-plugin and the channel manifest produced by
 * building the standard dist produces an installation consistent with what is produced by
 * the galleon-maven-plugin when it produced the standard dist.
 * <p>
 * The purpose of this test is to demonstrate consistency between the two provisioning methods,
 * thus supporting the concept that tests run against an installation provisioned one way are
 * meaningful for an installation provisioned the other way.
 */
public abstract class ProvisioningConsistencyBaseTest {

    private static final Logger log = Logger.getLogger(ProvisioningConsistencyBaseTest.class);

    private static final String INSTALLATION = ".installation";
    private static final String PROVISIONING = ".wildfly-maven-plugin-provisioning.xml";
    private static final Path JBOSS_HOME = resolveJBossHome();
    private final Path CHANNEL_INSTALLATION;
    private final Path INSTALLATION_METADATA;
    private final Path PROVISIONING_XML;
    private static final Path SOURCE_HOME = JBOSS_HOME.getParent().getParent().getParent().getParent().getParent();
    private static final Path DIST_INSTALLATION = JBOSS_HOME.getParent().resolve("wildfly-without-channel");

    private static Path resolveJBossHome() {
        try {
            return new File(System.getenv("JBOSS_HOME")).getCanonicalFile().toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ProvisioningConsistencyBaseTest(String targetDist) {
        CHANNEL_INSTALLATION = SOURCE_HOME.resolve(targetDist);
        INSTALLATION_METADATA = CHANNEL_INSTALLATION.resolve(INSTALLATION);
        PROVISIONING_XML = CHANNEL_INSTALLATION.resolve(PROVISIONING);
    }

    /**
     * This test case is not relevant when the test suite/module is executed against a distribution
     * externally given through the jboss.dist Maven property.
     */
    @BeforeClass
    public static void assumeCompatibleEnvironment() throws IOException {
        assumeJbossDistIsNotExternallySet();
    }

    private static void assumeJbossDistIsNotExternallySet() throws IOException {
        Path jbossDist = new File(System.getProperty("jboss.dist")).getCanonicalFile().toPath();
        Path defaultJbossDist = SOURCE_HOME.resolve(System.getProperty("build.output.dir"));
        Assume.assumeTrue(jbossDist.equals(defaultJbossDist));
    }

    /**
     * Compare the contents of an installation created using wildfly-maven-plugin
     * and a channel with the standard dist.
     *
     * @throws IOException if a problem occurs walking the file tree of the
     */
    @SuppressWarnings("JUnit3StyleTestMethodInJUnit4Class")
    @Test
    public void testInstallationEquivalence() throws IOException {
        final AtomicReference<Path> installationMetadata = new AtomicReference<>();
        final List<String> errors = new ArrayList<>();
        Files.walkFileTree(CHANNEL_INSTALLATION, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                log.trace("Testing " + dir);
                if (dir.equals(CHANNEL_INSTALLATION)) {
                    File distRoot = getDistFile(dir, true, true, errors);
                    Set<String> channelChildren = new TreeSet<>(Arrays.asList(Objects.requireNonNull(dir.toFile().list())));
                    channelChildren.remove(PROVISIONING);
                    Set<String> distChildren = new TreeSet<>(Arrays.asList(Objects.requireNonNull(distRoot.list())));
                    assertEquals(dir.toString(), channelChildren, distChildren);
                    return FileVisitResult.CONTINUE;
                } else if (dir.equals(INSTALLATION_METADATA)) {
                    installationMetadata.set(dir);
                    File[] files = dir.toFile().listFiles();
                    if (files == null || files.length == 0) {
                        errors.add("No children for " + dir);
                    }
                    File dist = getDistFile(dir, true, true, errors);
                    files = dist.listFiles();
                    if (files != null && files.length > 0) {
                        errors.add(String.format("%s has unexpected files: %s", dist, Arrays.asList(files)));
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    File distDir = getDistFile(dir, true, true, errors);
                    if (distDir != null) {
                        assertTrue(dir.toString(), Objects.deepEquals(dir.toFile().list(), distDir.list()));
                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                log.trace("Testing " + path);
                if (path.equals(PROVISIONING_XML)) {
                    getDistFile(path, false, false, errors);
                } else {
                    File distFile = getDistFile(path, true, false, errors);
                    if (distFile != null) {
                        if (path.toFile().length() != distFile.length()) {
                            log.trace("File size is different");
                            // This can happen on some platforms for fat jar generated at provisioning time.
                            // Check that the actual jar content is identical.
                            if (!sameJarContent(path, distFile.toPath(), errors)) {
                                errors.add(String.format("dist file for %s has an unexpected length: %d not %d",
                                        path, distFile.length(), path.toFile().length()));
                            }
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        if (installationMetadata.get() == null) {
            errors.add("No .installation directory found");
        }
        if (!errors.isEmpty()) {
            fail(errors.toString());
        }
    }

    private File getDistFile(Path channelPath, boolean exists, boolean directory, List<String> errors) {
        return getDistFile(CHANNEL_INSTALLATION, channelPath, DIST_INSTALLATION, exists, directory, errors);
    }

    private static File getDistFile(Path currentRoot, Path currentPath, Path distRoot, boolean exists, boolean directory, List<String> errors) {
        Path relative = currentRoot.relativize(currentPath);
        Path path = distRoot.resolve(relative);
        File test = path.toFile();
        File result = null;
        if (distRoot.equals(path)) {
            assertTrue(path + " does not exist", test.exists());
            assertTrue(path + " is not a directory", test.isDirectory());
            result = test;
        } else {
            if (exists) {
                if (!test.exists()) {
                    errors.add(String.format("%s %s does not exist", (directory ? "Directory" : "File"), path));
                } else if (test.isDirectory() != directory) {
                    errors.add(String.format("%s %s %s a directory",
                            (directory ? "Directory" : "File"),
                            path,
                            (directory ? "is" : "is not")));
                } else {
                    // Test file is acceptable; return it
                    result = test;
                }
            } else if (test.exists()) {
                errors.add(String.format("%s %s should not exist", (directory ? "Directory" : "File"), path));
            }
        }
        return result;
    }

    private static boolean sameJarContent(Path current, Path dist, List<String> errors) {
        if (!dist.getFileName().toString().endsWith(".jar")) {
            return false;
        }
        log.trace("Checking jar " + current);
        Path tempFolder = null;
        try {
            tempFolder = Files.createTempDirectory("checkconsistency");
            Path distUnzipped = tempFolder.resolve("dist");
            Path currentUnzipped = tempFolder.resolve("current");
            unzipFile(dist, distUnzipped);
            unzipFile(current, currentUnzipped);
            checkContent(currentUnzipped, distUnzipped, errors);
            if(errors.isEmpty()) return true;
        } catch (IOException ex) {
            errors.add("Exception occurred when checking " + dist + " file content. " + ex);
        } finally {
            try {
                if (tempFolder != null) {
                    FileUtils.deleteDirectory(tempFolder.toFile());
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return false;
    }

    private static void checkContent(Path currentRoot, Path dist, List<String> errors) throws IOException {
        Files.walkFileTree(currentRoot, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                log.trace("Testing " + dir);
                if (dir.equals(currentRoot)) {
                    File distRoot = getDistFile(currentRoot, dir, dist, true, true, errors);
                    Set<String> channelChildren = new TreeSet<>(Arrays.asList(Objects.requireNonNull(dir.toFile().list())));
                    Set<String> distChildren = new TreeSet<>(Arrays.asList(Objects.requireNonNull(distRoot.list())));
                    assertEquals(dir.toString(), channelChildren, distChildren);
                    return FileVisitResult.CONTINUE;
                } else {
                    File distDir = getDistFile(currentRoot, dir, dist, true, true, errors);
                    if (distDir != null) {
                        assertTrue(dir.toString(), Objects.deepEquals(dir.toFile().list(), distDir.list()));
                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                System.out.println("Checking " + path);
                File distFile = getDistFile(currentRoot, path, dist, true, false, errors);
                if (distFile == null) {
                    errors.add(String.format("current file %s has not been found in %s",
                            path, dist));
                } else {
                    try {
                        String hash1 = computeHash(path);
                        String hash2 = computeHash(distFile.toPath());
                        if (!hash1.equals(hash2)) {
                            errors.add(String.format("dist file for %s has an unexpected content",
                                    path));
                        }
                    } catch (Exception ex) {
                        errors.add(String.format("dist file for %s has an unexpected length: %d not %d",
                                    path, distFile.length(), path.toFile().length()));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
