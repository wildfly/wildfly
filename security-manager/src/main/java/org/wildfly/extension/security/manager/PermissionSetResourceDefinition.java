/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;

/**
 * Defines a resource that represents a set of security permissions.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class PermissionSetResourceDefinition extends SimplePersistentResourceDefinition {

    private String setName;

    private static final List<? extends SimplePersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(
            Arrays.asList(PermissionResourceDefinition.INSTANCE));


    PermissionSetResourceDefinition(final String setName) {
        super(PathElement.pathElement(setName, Constants.DEFAULT_VALUE), SecurityManagerExtension.getResolver(setName),
                new AbstractAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
        this.setName = setName;

    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    @Override
    public String getXmlElementName() {
        return this.setName;
    }
}
