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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.MISC;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.metadata.ModificationType.REMOVE;
import static org.jboss.as.patching.runner.TestUtils.createBundle0;
import static org.jboss.as.patching.runner.TestUtils.createModule0;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class ContentModificationUtils {
    public static ContentModification addModule(File patchDir, String patchElementID, String moduleName, String... resourceContents) throws IOException {
        File modulesDir = newFile(patchDir, patchElementID, MODULES);
        File moduleDir = createModule0(modulesDir, moduleName, resourceContents);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, newHash), NO_CONTENT, ADD);
        return moduleAdded;
    }

    public static ContentModification addModule(File patchDir, String patchElementID, String moduleName) throws IOException {
        File modulesDir = newFile(patchDir, patchElementID, MODULES);
        File moduleDir = createModule0(modulesDir, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, newHash), NO_CONTENT, ADD);
        return moduleAdded;
    }

    public static ContentModification removeModule(File existingModule) throws IOException {
        byte[] existingHash = hashFile(existingModule);
        return new ContentModification(new ModuleItem(existingModule.getName(), ModuleItem.MAIN_SLOT, NO_CONTENT), existingHash, REMOVE);
    }

    public static ContentModification modifyModule(File patchDir, String patchElementID, File existingModule, String newContent) throws IOException {
        byte[] existingHash = hashFile(existingModule);
        return modifyModule(patchDir, patchElementID, existingModule.getName(), existingHash, newContent);
    }

    public static ContentModification modifyModule(File patchDir, String patchElementID, String moduleName, byte[] existingHash, String newContent) throws IOException {
        File modulesDir = newFile(patchDir, patchElementID, MODULES);
        File modifiedModule = createModule0(modulesDir, moduleName, newContent);
        byte[] updatedHash = hashFile(modifiedModule);
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, updatedHash), existingHash, MODIFY);
        return moduleUpdated;
    }

    public static ContentModification modifyModule(File patchDir, String patchElementID, String moduleName, byte[] existingHash, TestUtils.ContentTask task) throws IOException {
        File modulesDir = newFile(patchDir, patchElementID, MODULES);
        File modifiedModule = createModule0(modulesDir, moduleName, task);
        byte[] updatedHash = hashFile(modifiedModule);
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, updatedHash), existingHash, MODIFY);
        return moduleUpdated;
    }

    public static ContentModification addBundle(File patchDir, String patchElementID, String bundleName) throws IOException {
        File bundlesDir = newFile(patchDir, patchElementID, BUNDLES);
        File bundleDir = createBundle0(bundlesDir, bundleName, randomString());
        byte[] newHash = hashFile(bundleDir);
        ContentModification bundleAdded = new ContentModification(new BundleItem(bundleName, null, newHash), NO_CONTENT, ADD);
        return bundleAdded;
    }

    public static ContentModification removeBundle(File existingBundle) throws IOException {
        byte[] existingHash = hashFile(existingBundle);
        return new ContentModification(new BundleItem(existingBundle.getName(), null, NO_CONTENT), existingHash, REMOVE);
    }

    public static ContentModification modifyBundle(File patchDir, String patchElementID, File existingBundle, String newContent) throws IOException {
        File bundlesDir = newFile(patchDir, patchElementID, BUNDLES);
        File modifiedBundle = createBundle0(bundlesDir, existingBundle.getName(), newContent);
        byte[] existingHash = hashFile(existingBundle);
        byte[] updatedHash = hashFile(modifiedBundle);
        ContentModification moduleUpdated = new ContentModification(new BundleItem(existingBundle.getName(), null, updatedHash), existingHash, MODIFY);
        return moduleUpdated;
    }

    public static ContentModification addMisc(File patchDir, String patchElementID, String content, String... fileSegments) throws IOException {
        File miscDir = newFile(patchDir, patchElementID, MISC);
        File addedFile = touch(miscDir, fileSegments);
        dump(addedFile, content);
        byte[] newHash = hashFile(addedFile);
        String[] subdir = new String[fileSegments.length -1];
        System.arraycopy(fileSegments, 0, subdir, 0, fileSegments.length - 1);
        ContentModification fileAdded = new ContentModification(new MiscContentItem(addedFile.getName(), subdir, newHash), NO_CONTENT, ADD);
        return fileAdded;
    }

    public static ContentModification removeMisc(File existingFile, String... fileSegments) throws IOException {
        byte[] existingHash = hashFile(existingFile);
        String[] subdir = new String[0];
        if (fileSegments.length > 0) {
            subdir = new String[fileSegments.length -1];
            System.arraycopy(fileSegments, 0, subdir, 0, fileSegments.length - 1);
        }
        ContentModification fileRemoved = new ContentModification(new MiscContentItem(existingFile.getName(), subdir, NO_CONTENT), existingHash, REMOVE);
        return fileRemoved;
    }

    public static ContentModification modifyMisc(File patchDir, String patchElementID, String modifiedContent, File existingFile, String... fileSegments) throws IOException {
        byte[] existingHash = hashFile(existingFile);
        return modifyMisc(patchDir, patchElementID, modifiedContent, existingHash, fileSegments);
    }

    public static ContentModification modifyMisc(File patchDir, String patchElementID, String modifiedContent, byte[] existingHash, String... fileSegments) throws IOException {
        File miscDir = newFile(patchDir, patchElementID, MISC);
        File modifiedFile = touch(miscDir, fileSegments);
        dump(modifiedFile, modifiedContent);
        byte[] modifiedHash = hashFile(modifiedFile);
        String[] subdir = new String[0];
        if (fileSegments.length > 0) {
            subdir = new String[fileSegments.length -1];
            System.arraycopy(fileSegments, 0, subdir, 0, fileSegments.length - 1);
        }
        ContentModification fileUpdated = new ContentModification(new MiscContentItem(modifiedFile.getName(), subdir, modifiedHash), existingHash, MODIFY);
        return fileUpdated;
    }

}
