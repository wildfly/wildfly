/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.scripts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.wildfly.test.common.ServerHelper;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum Shell {
    BASH(".sh", ".conf", () -> isShellSupported("bash", "-c", "echo", "test")),
    BATCH(".bat", ".conf.bat", TestSuiteEnvironment::isWindows),
    DASH(".sh", ".conf", () -> isShellSupported("dash", "-c", "echo", "test"), "dash"),
    KSH(".sh", ".conf", () -> isShellSupported("ksh", "-c", "echo", "test"), "ksh"),
    POWERSHELL(".ps1", ".conf.ps1", () -> isShellSupported("powershell", "-Help"),
            "powershell",
            "-ExecutionPolicy",
            "Unrestricted",
            "-NonInteractive",
            "-File"),
    ;
    private final String extension;
    private final String confExtension;
    private final BooleanSupplier supported;
    private final String[] prefix;

    Shell(final String extension, final String confExtension, final BooleanSupplier supported, final String... prefix) {
        this.extension = extension;
        this.confExtension = confExtension;
        this.supported = supported;
        this.prefix = prefix;
    }

    public String[] getPrefix() {
        return prefix;
    }

    public String getExtension() {
        return extension;
    }

    public String getConfExtension() {
        return confExtension;
    }

    public boolean isSupported() {
        return supported.getAsBoolean();
    }

    private static boolean isShellSupported(final String name, final String... args) {
        final List<String> cmd = new ArrayList<>();
        cmd.add(name);
        if (args != null && args.length > 0) {
            cmd.addAll(Arrays.asList(args));
        }
        try {
            final Path stdout = Files.createTempFile(name + "-supported", ".log");
            final ProcessBuilder builder = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(stdout.toFile());
            Process process = null;
            try {
                process = builder.start();
                if (!process.waitFor(ServerHelper.TIMEOUT, TimeUnit.SECONDS)) {
                    return false;
                }
                return process.exitValue() == 0;
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (process != null) {
                    process.destroyForcibly();
                }
                Files.deleteIfExists(stdout);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
