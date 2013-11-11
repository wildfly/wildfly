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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xnio.IoUtils;

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

    public void verify(final File jar) throws Exception {
        String[] args = {"-verify", "-verbose", jar.getAbsolutePath()};
        run(args);
    }

    private void run(String[] args) {
        try {
            Class<?> jdk7JarSignerClass = this.getClass().getClassLoader().loadClass("sun.security.tools.JarSigner");
            Object jdk7JarSigner = jdk7JarSignerClass.newInstance();
            Method run = jdk7JarSigner.getClass().getDeclaredMethod("run", String[].class);
            run.invoke(jdk7JarSigner, new Object[]{args});
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            //not jdk7, try jdk8
            runJDK8(args);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("could not sign", e);
        }
    }

    private void runJDK8(String[] args) {
        try {
            Class<?> jdk8JarSigner = this.getClass().getClassLoader().loadClass("sun.security.tools.jarsigner.Main");
            Method run = jdk8JarSigner.getMethod("main", String[].class);
            run.invoke(null, new Object[]{args});
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Could not find JarSigner to invoke", e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("could not sign", e);
        }

    }

    public void sign(final File jar, final File signedJar) throws IOException {
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
