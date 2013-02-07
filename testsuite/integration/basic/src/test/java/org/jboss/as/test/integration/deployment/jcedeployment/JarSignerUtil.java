/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.jcedeployment;

import org.xnio.IoUtils;
import sun.security.tools.JarSigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility for signing jars.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class JarSignerUtil {
    private File keystore;
    private String storePass;
    private String keyPass;
    private String alias;

    public JarSignerUtil(final File keystore, final String storePass, final String keyPass, final String alias) {
        this.keystore = keystore;
        this.storePass = storePass;
        this.keyPass = keyPass;
        this.alias = alias;
    }

    public void verify(final File jar) {
        String[] args = {"-verify", "-verbose", jar.getAbsolutePath()};
        final JarSigner jarSigner = new JarSigner();
        jarSigner.run(args);
    }

    public void sign(final File jar, final File signedJar) throws IOException {
        copyFile(jar, signedJar);
        sign(signedJar);
    }

    private void sign(final File jar) {
        String[] args = {
                "-keystore", keystore.getAbsolutePath(),
                "-storepass", storePass,
                "-keypass", keyPass,
                jar.getAbsolutePath(),
                alias
        };
        final JarSigner signer = new JarSigner();
        signer.run(args);
    }

    private static void copyFile(final File src, final File dst) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            IoUtils.safeClose(in);
            IoUtils.safeClose(out);
        }
    }

}
