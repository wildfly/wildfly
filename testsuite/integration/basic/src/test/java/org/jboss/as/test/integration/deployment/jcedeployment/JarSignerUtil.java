/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility for signing jars.
 *
 * @author Tomaz Cerar
 */
class JarSignerUtil {
    private File keystore;
    private String storePass;
    private String keyPass;
    private String alias;

    JarSignerUtil(final File keystore, final String storePass, final String keyPass, final String alias) {
        this.keystore = keystore;
        this.storePass = storePass;
        this.keyPass = keyPass;
        this.alias = alias;
    }

    public void verify(final File jar) throws Exception {
        String[] args = {"-verify", "-verbose", jar.getAbsolutePath()};
        run(args);
    }

    private void run(String[] args) {
        try {
            String home = System.getenv("JAVA_HOME");
            if (home == null) {
                home = System.getProperty("java.home");
            }

            String jarSigner = home + File.separator + "bin" + File.separator + "jarsigner";
            StringBuilder command = new StringBuilder(jarSigner);
            for (String a : args) {
                command.append(" ").append(a);
            }
            Process process = Runtime.getRuntime().exec(command.toString());
            int result = process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("could not sign", e);
        }
    }

    void sign(final File jar, final File signedJar) throws IOException {
        copyFile(jar, signedJar);
        try {
            sign(signedJar);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void sign(final File jar) throws Exception {
        String[] args = {
                "-keystore", keystore.getAbsolutePath(),
                "-storepass", storePass,
                "-keypass", keyPass,
                jar.getAbsolutePath(),
                alias
        };
        run(args);
    }

    private static void copyFile(final File src, final File dst) throws IOException {
        Files.copy(src.toPath(), dst.toPath());
    }

}
