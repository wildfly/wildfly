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

package org.jboss.as.patching.tests;

import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationBuilderTarget;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.jboss.as.patching.runner.TestUtils;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractPatchTestBuilder<T> extends ModificationBuilderTarget<T> {

    protected abstract String getPatchId();
    protected abstract File getPatchDir();
    protected abstract T returnThis();

    public T addFile(byte[] resultingHash, final String content, String... path) throws IOException {
        final ContentModification modification = ContentModificationUtils.addMisc(getPatchDir(), getPatchId(), content, path);
        return addContentModification(modification, resultingHash);
    }

    public T addFileWithRandomContent(byte[] resultingHash, String... path) throws IOException {
        return addFile(resultingHash, randomString(), path);
    }

    public T updateFileWithRandomContent(byte[] existingHash, byte[] resultingHash, String... path) throws IOException {
        final ContentModification modification = ContentModificationUtils.modifyMisc(getPatchDir(), getPatchId(), randomString(), Arrays.copyOf(existingHash, existingHash.length), path);
        return addContentModification(modification, resultingHash);
    }

    public T removeFile(byte[] existingHash, String... path) {
        final String name = path[path.length - 1];
        removeFile(name, Arrays.asList(Arrays.copyOf(path, path.length - 1)), existingHash, false);
        return returnThis();
    }

    public T addModuleWithContent(final String moduleName, byte[] resultingHash, final String... resourceContents) throws IOException {
        final ContentModification modification = ContentModificationUtils.addModule(getPatchDir(), getPatchId(), moduleName, resourceContents);
        return addContentModification(modification, resultingHash);
    }

    public T addModuleWithRandomContent(final String moduleName, byte[] resultingHash) throws IOException {
        final ContentModification modification = ContentModificationUtils.addModule(getPatchDir(), getPatchId(), moduleName, randomString());
        return addContentModification(modification, resultingHash);
    }

    public T updateModuleWithRandomContent(final String moduleName, byte[] existingHash, byte[] resultingHash) throws IOException {
        final ContentModification modification = ContentModificationUtils.modifyModule(getPatchDir(), getPatchId(), moduleName, existingHash, randomString());
        return addContentModification(modification, resultingHash);
    }

    public T updateModule(final String moduleName, byte[] existingHash, byte[] resultingHash, final TestUtils.ContentTask task) throws IOException {
        final ContentModification modification = ContentModificationUtils.modifyModule(getPatchDir(), getPatchId(), moduleName, existingHash, task);
        return addContentModification(modification, resultingHash);
    }

    protected T addContentModification(final ContentModification modification, byte[] resultingHash) {
        addContentModification(modification);
        contentHash(modification, resultingHash);
        return returnThis();
    }

    static void contentHash(final ContentModification modification, byte[] resultingHash) {
        if (resultingHash != null) {
            final byte[] contentHash = modification.getItem().getContentHash();
            System.arraycopy(contentHash, 0, resultingHash, 0, contentHash.length);
        }
    }

}
