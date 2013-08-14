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

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElementBuilder;
import org.jboss.as.patching.runner.ContentItemFilter;

/**
 * {@link PatchConfig} implementation.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class PatchConfigBuilder implements ContentItemFilter {

    public static enum AffectsType {
        UPDATED,
        ORIGINAL,
        BOTH,
        NONE
    }

    private String patchId = UUID.randomUUID().toString();
    private String description = "no patch description available";
    private String appliesToName;
    private String appliesToVersion;
    private String resultingVersion;
    private Patch.PatchType patchType;
    private boolean generateByDiff = true;
    private Set<String> runtimeUseItems = new HashSet<String>();
    private Set<ContentItem> specifiedContent = new HashSet<ContentItem>();
    private Map<String, PatchElementConfigBuilder> elements = new LinkedHashMap<String, PatchElementConfigBuilder>();

    PatchConfigBuilder setPatchId(String patchId) {
        this.patchId = patchId;
        return this;
    }

    PatchConfigBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    void setPatchType(Patch.PatchType patchType) {
        this.patchType = patchType;
    }

    PatchConfigBuilder setCumulativeType(String appliesToVersion, String resultingVersion) {
        this.patchType = Patch.PatchType.CUMULATIVE;
        this.appliesToVersion = appliesToVersion;
        this.resultingVersion = resultingVersion;
        return this;
    }

    PatchConfigBuilder setOneOffType(String appliesToVersion) {
        assert appliesToVersion != null : "appliesToVersion is null";

        this.patchType = Patch.PatchType.ONE_OFF;
        this.appliesToVersion = appliesToVersion;

        return this;
    }

    PatchConfigBuilder setGenerateByDiff(boolean generateByDiff) {
        this.generateByDiff = generateByDiff;
        return this;
    }

    public boolean isGeneratedByDiff() {
        return generateByDiff;
    }

    PatchConfigBuilder addRuntimeUseItem(String item) {
        this.runtimeUseItems.add(item);
        return this;
    }

    void setAppliesToName(String appliesToName) {
        this.appliesToName = appliesToName;
    }

    void setAppliesToVersion(String appliesToVersion) {
        this.appliesToVersion = appliesToVersion;
    }

    void setResultingVersion(String resultingVersion) {
        this.resultingVersion = resultingVersion;
    }

    Set<ContentItem> getSpecifiedContent() {
        return specifiedContent;
    }

    PatchElementConfigBuilder addElement(final String name) {
        final PatchElementConfigBuilder builder = new PatchElementConfigBuilder(name, this);
        if (elements.put(name, builder) != null) {
            throw processingError("duplicate layer %s", name);
        }
        return builder;
    }

    PatchConfig build() {
        return new PatchConfigImpl(new ArrayList<PatchElementConfig>(elements.values()));
    }

    class PatchConfigImpl implements PatchConfig {

        private Collection<PatchElementConfig> elements;

        PatchConfigImpl(Collection<PatchElementConfig> elements) {
            this.elements = elements;
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
        public Patch.PatchType getPatchType() {
            return patchType;
        }

        @Override
        public Set<String> getInRuntimeUseItems() {
            return runtimeUseItems;
        }

        @Override
        public String getAppliesToProduct() {
            return appliesToName;
        }

        @Override
        public String getAppliesToVersion() {
            return appliesToVersion;
        }

        @Override
        public String getResultingVersion() {
            return resultingVersion;
        }

        @Override
        public boolean isGenerateByDiff() {
            return generateByDiff;
        }

        @Override
        public Set<ContentItem> getSpecifiedContent() {
            return specifiedContent;
        }

        @Override
        public Collection<PatchElementConfig> getElements() {
            return elements;
        }

        @Override
        public PatchBuilderWrapper toPatchBuilder() {
            final PatchBuilderWrapper wrapper = new PatchBuilderWrapper() {
                @Override
                PatchElementBuilder modifyLayer(String name, boolean addOn) {
                    if (addOn) {
                        throw processingError("does not support add-ons %s", name);
                    }
                    final PatchElementConfigBuilder config = PatchConfigBuilder.this.elements.get(name);
                    if (config == null) {
                        throw processingError("missing patch-config for layer %s", name);
                    }
                    final PatchElementBuilder builder;
                    if (config.getPatchType() == null) {
                        config.setPatchType(patchType);
                    }
                    if (patchType == Patch.PatchType.CUMULATIVE) {
                        builder = upgradeElement(config.getPatchId(), name, false);
                    } else {
                        builder = oneOffPatchElement(config.getPatchId(), name, false);
                    }
                    if (config.getDescription() != null) {
                        builder.setDescription(config.getDescription());
                    }
                    builder.setContentItemFilter(config);
                    return builder;
                }
            };

            wrapper.setDescription(description);
            wrapper.setPatchId(patchId);
            wrapper.setContentItemFilter(PatchConfigBuilder.this);

            return wrapper;
        }
    }

    @Override
    public boolean accepts(ContentItem item) {
        if (generateByDiff) {
            return true;
        }
        if (item.getContentType() == ContentType.MISC) {
            for (final ContentItem s : specifiedContent) {
                if (accepts((MiscContentItem) item, (MiscContentItem) s)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean accepts(MiscContentItem one, MiscContentItem two) {
        return one.getName().equals(two.getName()) &&
                one.getRelativePath().equals(two.getRelativePath());
    }

}
