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

package org.jboss.as.patching.metadata;

import java.util.List;

import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.installation.InstalledIdentity;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchImpl implements Patch {

    private final String patchID;
    private final String description;
    private final Identity identity;
    private final List<PatchElement> elements;
    private final List<ContentModification> modifications;

    public PatchImpl(String patchID, String description, Identity identity,
                     List<PatchElement> elements, List<ContentModification> modifications) {

        if (!Patch.PATCH_NAME_PATTERN.matcher(patchID).matches()) {
            throw PatchMessages.MESSAGES.illegalPatchName(patchID);
        }

        this.patchID = patchID;
        this.description = description;
        this.identity = identity;
        this.elements = elements;
        this.modifications = modifications;
    }

    @Override
    public String getPatchId() {
        return patchID;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public List<PatchElement> getElements() {
        return elements;
    }

    @Override
    public List<ContentModification> getModifications() {
        return modifications;
    }

    public static class RollbackPatchImpl implements RollbackPatch {

        private final Patch patch;
        private final InstalledIdentity installedIdentity;
        public RollbackPatchImpl(Patch patch, InstalledIdentity installedIdentity) {
            this.patch = patch;
            this.installedIdentity = installedIdentity;
        }

        @Override
        public String getPatchId() {
            return patch.getPatchId();
        }

        @Override
        public String getDescription() {
            return patch.getDescription();
        }

        @Override
        public Identity getIdentity() {
            return patch.getIdentity();
        }

        @Override
        public List<PatchElement> getElements() {
            return patch.getElements();
        }

        @Override
        public List<ContentModification> getModifications() {
            return patch.getModifications();
        }

        @Override
        public InstalledIdentity getIdentityState() {
            return installedIdentity;
        }
    }

}
