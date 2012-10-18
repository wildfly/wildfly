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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public PatchBuilder toPatchBuilder() {
        PatchBuilder pb = PatchBuilder.create()
                .setPatchId(getPatchId())
                .setDescription(getDescription())
                .setPatchType(getPatchType())
                .setResultingVersion(getResultingVersion());
        for (String applyTo : appliesTo) {
            pb.addAppliesTo(applyTo);
        }
        return pb;
    }

    void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setPatchType(Patch.PatchType patchType) {
        this.patchType = patchType;
    }

    void setResultingVersion(String resultingVersion) {
        this.resultingVersion = resultingVersion;
    }

    void addAppliesTo(String appliesTo) {
        this.appliesTo.add(appliesTo);
    }

    void setGenerateByDiff(boolean generateByDiff) {
        this.generateByDiff = generateByDiff;
    }

    public void addRuntimeUseItem(DistributionContentItem item) {
        this.runtimeUseItems.add(item);
    }

}
