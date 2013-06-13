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

package org.jboss.as.patching.management;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * @author Emanuel Muckenhuber
 */
class PatchHistory {

    private final List<PatchHistoryEntry> entries;
    private PatchHistory(List<PatchHistoryEntry> entries) {
        this.entries = entries;
    }

    /**
     * Load the patch history from the disk.
     *
     * @param currentPatchId the current patch id
     * @param image          the installed image
     * @return the patch history
     * @throws IOException
     */
    static PatchHistory loadHistory(final String currentPatchId, InstalledImage image) throws IOException {
        final ArrayList<PatchHistoryEntry> entries = new ArrayList<PatchHistoryEntry>();
        PatchHistoryEntry current = loadHistoryEntry(currentPatchId, image);
        if (current != null) {
            // Exclude the current entry
            while ((current = current.getPreviousEntry(image)) != null ) {
                entries.add(current);
            }
        }
        return new PatchHistory(entries);
    }


    static class PatchHistoryEntry {

        private final String base;          // The current id
        private final String releaseID;     // The previous id
        private final String timestamp;     // The applied timestamp
        private final List<String> patches; // The active patches

        PatchHistoryEntry(final String base, final String releaseID,
                          final String timestamp, final List<String> patches) {
            this.base = base;
            this.releaseID = releaseID;
            this.timestamp = timestamp;
            this.patches = patches;
        }

        protected PatchHistoryEntry getPreviousEntry(final InstalledImage image) throws IOException {
            return loadHistoryEntry(releaseID, image);
        }

    }

    static PatchHistoryEntry loadHistoryEntry(final String patchID, final InstalledImage image) throws IOException {
        if (Constants.BASE.equals(patchID)) {
            // no history
            return null;
        }
        final File history = image.getPatchHistoryDir(patchID);
        if (! history.exists()) {
            return null;
        }
        final File previous = new File(history, Constants.INSTALLATION_METADATA);
        if (! previous.exists()) {
            // This might be an error condition?
            return null;
        }

        // Load the information from the disk
        final Properties properties = PatchUtils.loadProperties(previous);
        final String releaseId = PatchUtils.readRef(properties, Constants.RELEASE_PATCH_ID);
        final List<String> patches = PatchUtils.readRefs(properties);

        final File timestampFile = new File(history, Constants.TIMESTAMP);
        final String timestamp = PatchUtils.readRef(timestampFile);

        return new PatchHistoryEntry(patchID, releaseId, timestamp, patches);
    }

}
