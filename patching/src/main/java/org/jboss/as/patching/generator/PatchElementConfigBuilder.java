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

package org.jboss.as.patching.generator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.runner.ContentItemFilter;

/**
 * @author Emanuel Muckenhuber
 */
class PatchElementConfigBuilder implements PatchElementConfig, ContentItemFilter {

    private final String layerName;
    private final PatchConfigBuilder parent;

    private Patch.PatchType patchType;
    private String patchId = UUID.randomUUID().toString();
    private String description = "no description available";
    private final Set<ContentItem> specifiedContents = new HashSet<ContentItem>();

    PatchElementConfigBuilder(String layerName, PatchConfigBuilder parent) {
        this.layerName = layerName;
        this.parent = parent;
    }

    @Override
    public String getPatchId() {
        return patchId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getLayerName() {
        return layerName;
    }

    @Override
    public Patch.PatchType getPatchType() {
        return patchType;
    }

    void setPatchType(Patch.PatchType patchType) {
        this.patchType = patchType;
    }

    void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Set<ContentItem> getSpecifiedContent() {
        return specifiedContents;
    }

    @Override
    public boolean accepts(ContentItem item) {
        if (item.getContentType() != ContentType.MISC) {
            for (final ContentItem s : specifiedContents) {
                if (accepts((ModuleItem) s, (ModuleItem) item)) {
                    return true;
                }
            }
        }
        if (parent.isGeneratedByDiff()) {
            return true;
        }
        return false;
    }

    static boolean accepts(ModuleItem one, ModuleItem two) {
        return one.getName().equals(two.getName())
                && one.getSlot().equals(two.getSlot());
    }

}
