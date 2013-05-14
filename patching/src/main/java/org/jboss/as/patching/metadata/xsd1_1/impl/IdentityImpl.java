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
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.patching.metadata.xsd1_1.Identity;


/**
 * @author Alexey Loubyansky
 *
 */
public class IdentityImpl implements Identity, RequiresCallback {

    private final String name;
    private final String version;
    private Collection<String> requires = Collections.emptyList();

    public IdentityImpl(String name, String version) {
        if(name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if(version == null) {
            throw new IllegalArgumentException("version is null");
        }
        this.name = name;
        this.version = version;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.Identity#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.Identity#getVersion()
     */
    @Override
    public String getVersion() {
        return version;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.patching.metadata.xsd1_1.Identity#getIncludes()
     */
    @Override
    public Collection<String> getRequires() {
        return requires;
    }

    @Override
    public void require(String patchId) {
        if(patchId == null) {
            throw new IllegalArgumentException("patchId is null");
        }
        if(requires.isEmpty()) {
            requires = new ArrayList<String>();
        }
        requires.add(patchId);
    }
}
