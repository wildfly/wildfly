/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.runner;

import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Adding or updating a module will add a module in the patch overlay directory {@linkplain org.jboss.as.boot.DirectoryStructure#getModulePatchDirectory(String)}.
 *
 * @author Emanuel Muckenhuber
 */
class ModuleUpdateTask extends AbstractModuleTask {

    ModuleUpdateTask(ModuleItem item, byte[] expected) {
        super(item, expected);
    }

    @Override
    public void execute(PatchingContext context) throws IOException {

        // Copy the new module resources to the patching directory
        final File targetDir = context.getModulePatchDirectory(item);
        final File sourceDir = context.getLoader().getFile(item);
        final File[] moduleResources = sourceDir.listFiles();
        for(final File file : moduleResources) {
            final File target = new File(targetDir, file.getName());
            copy(file, target);
        }
        // Hmm we actually don't need to do anything when rolling back the patch?
        // final ContentModification modification = new ContentModification(item, expected, ModificationType.MODIFY);
        // context.recordRollbackAction(modification);
    }

    static byte[] copy(File source, File target) throws IOException {
        final FileInputStream is = new FileInputStream(source);
        try {
            byte[] backupHash = copy(is, target);
            is.close();
            return backupHash;
        } finally {
            PatchUtils.safeClose(is);
        }
    }

    static byte[] copy(final InputStream is, final File target) throws IOException {
        if(! target.getParentFile().exists()) {
            target.getParentFile().mkdirs(); // Hmm
        }
        final OutputStream os = new FileOutputStream(target);
        try {
            byte[] nh = PatchUtils.copyAndGetHash(is, os);
            os.close();
            return nh;
        } finally {
            PatchUtils.safeClose(os);
        }
    }

}
