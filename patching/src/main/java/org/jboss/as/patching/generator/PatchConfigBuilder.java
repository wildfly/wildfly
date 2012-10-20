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

package org.jboss.as.patching.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;

/**
 * {@link PatchConfig} implementation.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class PatchConfigBuilder implements PatchConfig {

    private String patchId;
    private String description;
    private String resultingVersion;
    private Patch.PatchType patchType;
    private boolean generateByDiff;
    private List<String> appliesTo = new ArrayList<String>();
    private Set<DistributionContentItem> runtimeUseItems = new HashSet<DistributionContentItem>();
    private final Map<DistributionContentItem.Type, Map<ModificationType, SortedSet<DistributionContentItem>>> modifications =
            new HashMap<DistributionContentItem.Type, Map<ModificationType, SortedSet<DistributionContentItem>>>();

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
    public String getResultingVersion() {
        return resultingVersion;
    }

    @Override
    public List<String> getAppliesTo() {
        return appliesTo;
    }

    @Override
    public Set<DistributionContentItem> getInRuntimeUseItems() {
        return Collections.unmodifiableSet(runtimeUseItems);
    }

    public boolean isGenerateByDiff() {
        return generateByDiff;
    }

    @Override
    public Map<DistributionContentItem.Type, Map<ModificationType, SortedSet<DistributionContentItem>>> getSpecifiedContent() {
        return modifications;
    }

    @Override
    public PatchBuilder toPatchBuilder() {
        PatchBuilder pb = PatchBuilder.create()
                .setPatchId(getPatchId())
                .setDescription(getDescription());
        if (patchType == Patch.PatchType.ONE_OFF) {
            pb.setOneOffType(appliesTo);
        } else {
            pb.setCumulativeType(appliesTo.iterator().next(), resultingVersion);
        }

        return pb;
    }

    PatchConfigBuilder setPatchId(String patchId) {
        this.patchId = patchId;

        return this;
    }

    PatchConfigBuilder setDescription(String description) {
        this.description = description;

        return this;
    }

    PatchConfigBuilder setCumulativeType(String appliesToVersion, String resultingVersion) {
        this.patchType = Patch.PatchType.CUMULATIVE;
        this.appliesTo = Collections.singletonList(appliesToVersion);
        this.resultingVersion = resultingVersion;

        return this;
    }

    PatchConfigBuilder setOneOffType(List<String> appliesTo) {
        this.patchType = Patch.PatchType.ONE_OFF;
        this.appliesTo = Collections.unmodifiableList(appliesTo);

        return this;
    }

    PatchConfigBuilder setGenerateByDiff(boolean generateByDiff) {
        this.generateByDiff = generateByDiff;

        return this;
    }

    PatchConfigBuilder addRuntimeUseItem(DistributionContentItem item) {
        this.runtimeUseItems.add(item);

        return this;
    }

    PatchConfigBuilder addModification(DistributionContentItem item, ModificationType modificationType) {
        Map<ModificationType, SortedSet<DistributionContentItem>> typeMap;
        DistributionContentItem.Type itemType = item.getType();
        switch (itemType) {
            case MODULE_ROOT:
            case BUNDLE_ROOT:
            case MISC:
                typeMap = modifications.get(itemType);
                if (typeMap == null) {
                    typeMap = new HashMap<ModificationType, SortedSet<DistributionContentItem>>();
                    modifications.put(itemType, typeMap);
                }
                break;
            default:
                throw new IllegalArgumentException(itemType + " is not a valid content item type for a modification");
        }

        SortedSet<DistributionContentItem> items = typeMap.get(modificationType);
        if (items == null) {
            items = new TreeSet<DistributionContentItem>();
            typeMap.put(modificationType, items);
        }

        items.add(item);

        return this;
    }

}
