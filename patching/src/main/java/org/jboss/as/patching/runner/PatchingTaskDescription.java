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

import java.util.Arrays;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.ModificationType;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingTaskDescription {

    private final String patchId;
    private final ContentModification modification;
    private final PatchContentLoader loader;

    private final boolean conflicts;
    private final boolean skipIfTheSame;

    PatchingTaskDescription(String patchId, ContentModification modification, PatchContentLoader loader,
                            boolean conflicts, boolean skipIfExists) {
        this.patchId = patchId;
        this.modification = modification;
        this.loader = loader;
        this.conflicts = conflicts;
        this.skipIfTheSame = skipIfExists;
    }

    public String getPatchId() {
        return patchId;
    }

    public ContentModification getModification() {
        return modification;
    }

    public ModificationType getModificationType() {
        return modification.getType();
    }

    public ContentItem getContentItem() {
        return modification.getItem();
    }

    public <T extends ContentItem> T getContentItem(Class<T> expected) {
        return modification.getItem(expected);
    }

    public ContentType getContentType() {
        return modification.getItem().getContentType();
    }

    public PatchContentLoader getLoader() {
        return loader;
    }

    public boolean hasConflicts() {
        return conflicts;
    }

    public boolean skipIfTheSame() {
        return skipIfTheSame;
    }

    static PatchingTaskDescription create(final PatchingTasks.ContentTaskDefinition definition, final PatchContentLoader loader) {
        final ContentModification modification = resolveDefinition(definition);

        // Check if we already have the new content
        final ContentItem item = definition.getTarget().getItem();
        final byte[] currentHash = definition.getLatest().getTargetHash();
        final byte[] newContentHash = item.getContentHash();
        boolean skipIfExists = Arrays.equals(currentHash, newContentHash);

        return new PatchingTaskDescription(definition.getTarget().getPatchId(), modification, loader, definition.hasConflicts(), skipIfExists);

    }

    static ContentModification resolveDefinition(final PatchingTasks.ContentTaskDefinition definition) {
        // Only available in a single patch, yay!
        if(definition.getLatest() == definition.getTarget()) {
            return definition.getTarget().getModification();
        }

        // Create a new modification replacing the latest
        final ContentItem backupItem = definition.getTarget().getItem();
        final ContentModification modification = definition.getTarget().getModification();
        final byte[] target = definition.getLatest().getTargetHash();
        return new ContentModification(backupItem, target, modification.getType());
    }

}
