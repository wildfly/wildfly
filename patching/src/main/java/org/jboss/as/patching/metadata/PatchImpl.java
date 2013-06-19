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

import java.util.Collection;
import java.util.List;

import org.jboss.as.patching.installation.InstalledIdentity;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchImpl implements Patch {

    private final String patchID;
    private final String description;
    private final PatchType patchType;
    private final Identity identity;
    private final String resultingVersion;
    private final Collection<String> appliesTo;
    private final Collection<String> incompatibleWith;
    private final List<PatchElement> elements;
    private final List<ContentModification> modifications;

    public PatchImpl(String patchID, String description, PatchType patchType, Identity identity, String resultingVersion,
                     Collection<String> appliesTo, Collection<String> incompatibleWith, List<PatchElement> elements,
                     List<ContentModification> modifications) {
        this.patchID = patchID;
        this.description = description;
        this.patchType = patchType;
        this.identity = identity;
        this.resultingVersion = resultingVersion;
        this.appliesTo = appliesTo;
        this.incompatibleWith = incompatibleWith;
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
    public PatchType getPatchType() {
        return patchType;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public String getResultingVersion() {
        return resultingVersion;
    }

    @Override
    public Collection<String> getAppliesTo() {
        return appliesTo;
    }

    @Override
    public Collection<String> getIncompatibleWith() {
        return incompatibleWith;
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
        public PatchType getPatchType() {
            return patch.getPatchType();
        }

        @Override
        public Identity getIdentity() {
            return patch.getIdentity();
        }

        @Override
        public String getResultingVersion() {
            return patch.getResultingVersion();
        }

        @Override
        public Collection<String> getAppliesTo() {
            return patch.getAppliesTo();
        }

        @Override
        public Collection<String> getIncompatibleWith() {
            return patch.getIncompatibleWith();
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
