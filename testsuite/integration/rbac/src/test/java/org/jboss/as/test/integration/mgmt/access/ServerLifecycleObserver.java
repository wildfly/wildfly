/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mgmt.access;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Observers container lifecycle events to prepare the server with metadata needed to initialize installer subsystem.
 */
public class ServerLifecycleObserver {

    public void containerBeforeStart(@Observes BeforeStart event) throws IOException {
        final String jbossHomeProp = System.getProperty("jboss.home");
        final Path jbossHome = Path.of(jbossHomeProp);
        final Path installationDir = jbossHome.resolve(".installation");
        Files.createDirectory(installationDir);
        Files.writeString(installationDir.resolve("manifest.yaml"), "schemaVersion: 1.0.0");
        Files.createFile(installationDir.resolve("installer-channels.yaml"));

    }

    public void containerAfterStop(@Observes AfterStop event) throws IOException {
        final String jbossHomeProp = System.getProperty("jboss.home");
        final Path jbossHome = Path.of(jbossHomeProp);
        final Path installationDir = jbossHome.resolve(".installation");
        try (Stream<Path> paths = Files.walk(installationDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
