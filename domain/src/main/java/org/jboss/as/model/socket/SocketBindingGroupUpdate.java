/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.socket;

import java.util.Set;

import org.jboss.as.model.AbstractModelElementUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.UpdateFailedException;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingGroupUpdate extends AbstractModelElementUpdate<SocketBindingGroupElement> {

    private static final long serialVersionUID = -7177764052517325211L;
    private final String name;
    private final String defaultInterface;
    private final Set<String> includedGroups;

    public SocketBindingGroupUpdate(String name, String defaultInterface, Set<String> includedGroups) {
        this.name = name;
        this.defaultInterface = defaultInterface;
        this.includedGroups = includedGroups;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    /** {@inheritDoc} */
    public SocketBindingGroupUpdate getCompensatingUpdate(SocketBindingGroupElement original) {
        return new SocketBindingGroupUpdate(name, original.getDefaultInterface(), original.getIncludedSocketBindingGroups());
    }

    /** {@inheritDoc} */
    public Class<SocketBindingGroupElement> getModelElementType() {
        return SocketBindingGroupElement.class;
    }

    /** {@inheritDoc} */
    public void applyUpdate(SocketBindingGroupElement element) throws UpdateFailedException {
        element.setDefaultInterface(defaultInterface);
        for(final String groupName : includedGroups) {
            element.addIncludedGroup(groupName);
        }
    }

}
