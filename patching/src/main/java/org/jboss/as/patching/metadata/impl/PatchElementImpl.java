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

package org.jboss.as.patching.metadata.impl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchElementProvider;

/**
 * @author Alexey Loubyansky
 *
 */
public class PatchElementImpl implements PatchElement {

    private final String id;
    private String description = "no description";
    private PatchElementProvider provider;
    private final List<ContentModification> modifications = new ArrayList<ContentModification>();

    public PatchElementImpl(String id) {
        assert id != null;
        if (!Patch.PATCH_NAME_PATTERN.matcher(id).matches()) {
            throw PatchLogger.ROOT_LOGGER.illegalPatchName(id);
        }
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.PatchElement#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.PatchElement#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    public PatchElementImpl setDescription(String descr) {
        this.description = descr;
        return this;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.PatchElement#getProvider()
     */
    @Override
    public PatchElementProvider getProvider() {
        return provider;
    }

    public PatchElementImpl setProvider(PatchElementProvider provider) {
        assert provider != null;
        this.provider = provider;
        return this;
    }

    @Override
    public List<ContentModification> getModifications() {
        return modifications;
    }

    public PatchElementImpl addContentModification(ContentModification modification) {
        this.modifications.add(modification);
        return this;
    }

}
