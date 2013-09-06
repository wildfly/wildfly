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

package org.jboss.as.controller.access;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;

/**
 * An attribute of the resource that is the target of an action for which access control is needed.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class TargetAttribute {

    private static final List<AccessConstraintDefinition> NO_CONSTRAINTS = Collections.emptyList();

    private final String attributeName;
    private final TargetResource targetResource;
    private final AttributeAccess attributeAccess;

    private final ModelNode currentValue;

    public TargetAttribute(String attributeName, AttributeAccess attributeAccess, ModelNode currentValue, TargetResource targetResource) {
        this.attributeName = attributeName;
        this.targetResource = targetResource;
        this.currentValue = currentValue;
        this.attributeAccess = attributeAccess;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public TargetResource getTargetResource() {
        return targetResource;
    }

    /**
     * Get the access type.
     *
     * @return the access type
     */
    public AttributeAccess.AccessType getAccessType() {
        return attributeAccess == null ? null : attributeAccess.getAccessType();
    }

    /**
     * Get the storage type.
     *
     * @return the storage type
     */
    public AttributeAccess.Storage getStorageType() {
        return attributeAccess == null ? null : attributeAccess.getStorageType();
    }

    public AttributeDefinition getAttributeDefinition() {
        return attributeAccess == null ? null : attributeAccess.getAttributeDefinition();
    }

    /**
     * Gets the flags associated with this attribute.
     * @return the flags. Will not return {@code null}
     */
    public Set<AttributeAccess.Flag> getFlags() {
        if (attributeAccess == null) {
            return Collections.emptySet();
        }
        return attributeAccess.getFlags();
    }

    public ModelNode getCurrentValue() {
        return currentValue;
    }

    public ServerGroupEffect getServerGroupEffect() {
        return targetResource.getServerGroupEffect();
    }

    public HostEffect getHostEffect() {
        return targetResource.getHostEffect();
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        AttributeDefinition def = getAttributeDefinition();
        return def != null ? def.getAccessConstraints() : NO_CONSTRAINTS;
    }
}
