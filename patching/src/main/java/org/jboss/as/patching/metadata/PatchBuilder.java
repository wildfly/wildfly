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

package org.jboss.as.patching.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchBuilder implements Patch {

    private String patchId;
    private String description;
    private String resultingVersion;
    private PatchType patchType;
    private List<String> appliesTo = new ArrayList<String>();
    private List<ContentModification> modifications = new ArrayList<ContentModification>();

    @Override
    public String getPatchId() {
        return patchId;
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
    public String getResultingVersion() {
        return resultingVersion;
    }

    @Override
    public List<String> getAppliesTo() {
        return appliesTo;
    }

    @Override
    public List<ContentModification> getModifications() {
        return modifications;
    }

    void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setPatchType(PatchType patchType) {
        this.patchType = patchType;
    }

    public void setResultingVersion(String resultingVersion) {
        this.resultingVersion = resultingVersion;
    }

    void addAppliesTo(String appliesTo) {
        this.appliesTo.add(appliesTo);
    }

    void addContentModification(ContentModification modification) {
        this.modifications.add(modification);
    }

}
