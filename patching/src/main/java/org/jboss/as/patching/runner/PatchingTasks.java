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


import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingTasks {

    /**
     * Only accept misc items for rollback operations
     */
    static final ContentItemFilter ROLLBACK_FILTER = ContentItemFilter.MISC_ONLY;

    /**
     * Process multiple patches for rollback, trying to determine the current and target state for this applying this combination.
     *
     * This will track changes by location, trying to merge multiple changes per location and recording whether conflicts
     * were detected or not. It's up to the user to skip the content validation steps.
     *
     * @param structure the directory structure
     * @param patchId the patch-id to rollback
     * @param modifications the modifications by lcoation
     * @throws XMLStreamException
     * @throws IOException
     */
    static void rollback(final DirectoryStructure structure, final String patchId, final Map<Location, ContentTaskDefinition> modifications) throws XMLStreamException, IOException {
        // TODO perhaps use a separate format for keeping this information
        final Patch originalPatch = loadPatchInformation(patchId, structure);
        final Patch rollbackPatch = loadRollbackInformation(patchId, structure);
        // skip all module items for rollback
        rollback(patchId, originalPatch, rollbackPatch, modifications, ROLLBACK_FILTER);
    }

    /**
     * Compare the recorded rollback history and the original patch.xml to detect conflicts.
     *
     * @param patchId the patch id
     * @param originalPatch the original patch.xml
     * @param rollbackPatch the reverse (history) patch.xml
     * @param modifications the modifications
     * @param filter a generic content-item filter
     */
    static void rollback(final String patchId, final Patch originalPatch, final Patch rollbackPatch, final Map<Location, ContentTaskDefinition> modifications, ContentItemFilter filter) {

        // Process the original patch information
        final Map<Location, ContentModification> originalModifications = new HashMap<Location, ContentModification>();
        for(final ContentModification modification : originalPatch.getModifications()) {
            originalModifications.put(new Location(modification.getItem()), modification);
        }
        // Process the rollback information
        for(final ContentModification modification : rollbackPatch.getModifications()) {

            final ContentItem item = modification.getItem();
            // Skip module items when rolling back
            if(! filter.accepts(item)) {
                continue;
            }

            final Location location = new Location(item);
            final ContentModification original = originalModifications.remove(location);
            if(original == null) {
                if(modification.getType() != ModificationType.ADD) {
                    throw new IllegalStateException(item.toString()); // Only for development purpose
                }
            }

            final ContentEntry contentEntry = new ContentEntry(patchId, modification);
            ContentTaskDefinition definition = modifications.get(location);
            if(definition == null) {
                definition = new ContentTaskDefinition(location, contentEntry);
                modifications.put(location, definition);
            } else {
                // TODO perhaps we don't need check that
                boolean strict = true; // Strict history checks
                if(strict) {
                    // Check if the consistency of the history
                    final ContentEntry previous = definition.getTarget();
                    final byte[] hash = previous.getItem().getContentHash();
                    if(! Arrays.equals(hash, contentEntry.getTargetHash())) {
                        throw new IllegalStateException();
                    }
                }
                //
                definition.setTarget(contentEntry);
            }
            if(original == null) {
                continue;
            }

            // Check if the current content was the original item (preserve)
            final byte[] currentContent = modification.getTargetHash();
            final byte[] originalContent = original.getItem().getContentHash();

            if(! Arrays.equals(currentContent, originalContent)) {
                definition.addConflict(contentEntry);
            } else {
                // Check if backup item was the targeted one (override)
                final byte[] backupItem = item.getContentHash();
                final byte[] originalTarget = original.getTargetHash();
                //
                if(! Arrays.equals(backupItem, originalTarget))  {
                    definition.addConflict(contentEntry);
                }
            }
        }
    }

    /**
     * Simply a apply a patch, without requiring the
     *
     * @param patch
     * @param modifications
     * @throws XMLStreamException
     * @throws IOException
     */
    static void apply(final Patch patch, final Map<Location, ContentTaskDefinition> modifications) throws XMLStreamException, IOException {
        final String patchId = patch.getPatchId();
        for(final ContentModification modification : patch.getModifications()) {

            final ContentItem item = modification.getItem();
            final Location location = new Location(item);

            final ContentEntry contentEntry = new ContentEntry(patchId, modification);
            ContentTaskDefinition definition = modifications.get(location);
            if(definition == null) {
                definition = new ContentTaskDefinition(location, contentEntry);
                modifications.put(location, definition);
            }
            definition.setTarget(contentEntry);
        }
    }

    static class ContentTaskDefinition {

        private final Location location;
        private final ContentEntry latest;
        private ContentEntry target;
        private final List<ContentEntry> conflicts = new ArrayList<ContentEntry>();

        ContentTaskDefinition(Location location, ContentEntry latest) {
            this.location = location;
            this.latest = latest;
            this.target = latest;
        }

        public Location getLocation() {
            return location;
        }

        public ContentEntry getLatest() {
            return latest;
        }

        public ContentEntry getTarget() {
            return target;
        }

        public boolean hasConflicts() {
            return ! conflicts.isEmpty();
        }

        public List<ContentEntry> getConflicts() {
            return conflicts;
        }

        void setTarget(final ContentEntry entry) {
            target = entry;
        }

        void addConflict(ContentEntry entry) {
            conflicts.add(entry);
        }

    }

    static class ContentEntry {

        final String patchId;
        final ContentModification modification;

        ContentEntry(String patchId, ContentModification modification) {
            this.patchId = patchId;
            this.modification = modification;
        }

        public String getPatchId() {
            return patchId;
        }

        public ContentModification getModification() {
            return modification;
        }

        public ContentItem getItem() {
            return modification.getItem();
        }

        public byte[] getTargetHash() {
            return modification.getTargetHash();
        }

    }

    static Patch loadPatchInformation(final String patchId, final DirectoryStructure structure) throws IOException, XMLStreamException {
        final File patchDir = structure.getPatchDirectory(patchId);
        final File patchXml = new File(patchDir, PatchXml.PATCH_XML);
        return PatchXml.parse(patchXml);
    }

    static Patch loadRollbackInformation(final String patchId, final DirectoryStructure structure) throws IOException, XMLStreamException {
        final File historyDir = structure.getHistoryDir(patchId);
        final File patchXml = new File(historyDir, PatchingContext.ROLLBACK_XML);
        return PatchXml.parse(patchXml);
    }

}
