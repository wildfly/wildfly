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

package org.jboss.as.patching.metadata.xsd1_1.impl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.xsd1_1.PatchElement;
import org.jboss.as.patching.metadata.xsd1_1.PatchElementProvider;

/**
 * @author Alexey Loubyansky
 *
 */
public class PatchElementImpl implements PatchElement, UpgradeCallback {

    private final String id;
    private String descr;
    private PatchElementProvider provider;
    private PatchType patchType;
    private String resultingVersion;

    private final List<ContentModification> modifications = new ArrayList<ContentModification>();

    public PatchElementImpl(String id) {
        if(id == null) {
            throw new IllegalArgumentException("id is null");
        }
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getDescription()
     */
    @Override
    public String getDescription() {
        return descr;
    }

    public void setDescription(String descr) {
        this.descr = descr;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getProvider()
     */
    @Override
    public PatchElementProvider getProvider() {
        return provider;
    }

    public void setProvider(PatchElementProvider provider) {
        if(provider == null) {
            throw new IllegalArgumentException("provider is null");
        }
        this.provider = provider;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getPatchType()
     */
    @Override
    public PatchType getPatchType() {
        return patchType;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getResultingVersion()
     */
    @Override
    public String getResultingVersion() {
        if(patchType == null) {
            return null;
        }
        if(patchType == PatchType.ONE_OFF) {
            if(provider == null) {
                return null;
            }
            return provider.getVersion();
        }
        return resultingVersion;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.PatchElement#getModifications()
     */
    @Override
    public List<ContentModification> getModifications() {
        return modifications;
    }

    public void addContentModification(ContentModification modification) {
        this.modifications.add(modification);
    }

    @Override
    public PatchElementImpl setUpgrade(String version) {
        if(version == null) {
            throw new IllegalArgumentException("version is null");
        }
        this.resultingVersion = version;
        this.patchType = PatchType.CUMULATIVE;
        return this;
    }

    @Override
    public PatchElementImpl setNoUpgrade() {
        this.patchType = PatchType.ONE_OFF;
        return this;
    }
}
