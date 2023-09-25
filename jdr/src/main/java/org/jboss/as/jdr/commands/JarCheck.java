/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.commands;


import org.jboss.as.jdr.util.Utils;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.automount.Automounter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;

public class JarCheck extends JdrCommand {

    StringBuilder buffer;

    @Override
    public void execute() throws Exception {
        this.buffer = new StringBuilder();
        walk(VFS.getChild(this.env.getJbossHome()));
        this.env.getZip().add(this.buffer.toString(), "jarcheck.txt");
    }

    private void walk(VirtualFile root) throws NoSuchAlgorithmException {
        for(VirtualFile f : root.getChildren()) {
            if(f.isDirectory()) {
                walk(f);
            }
            else {
                check(f);
            }
        }
    }

    private void check(VirtualFile f) throws NoSuchAlgorithmException {
        try {
            MessageDigest alg = MessageDigest.getInstance("md5");
            byte [] buffer = Utils.toBytes(f);
            alg.update(buffer);
            String sum = new BigInteger(1, alg.digest()).toString(16);
            this.buffer.append(
                    f.getPathName().replace(this.env.getJbossHome(), "JBOSSHOME") + "\n"
                    + sum + "\n"
                    + getManifestString(f) + "===");
        }
        catch( java.util.zip.ZipException ze ) {
            // skip
        }
        catch( java.io.FileNotFoundException fnfe ) {
            ROOT_LOGGER.debug(fnfe);
        }
        catch( java.io.IOException ioe ) {
            ROOT_LOGGER.debug(ioe);
        }
    }

    private String getManifestString(VirtualFile file) throws java.io.IOException {
        try {
            Automounter.mount(file);
            String result = Utils.toString(file.getChild(Utils.MANIFEST_NAME));
            return result != null? result: "";
        } catch (Exception npe) {
            ROOT_LOGGER.tracef("no MANIFEST present");
            return "";
        } finally {
            if(Automounter.isMounted(file)){
                Automounter.cleanup(file);
            }
        }
    }
}
