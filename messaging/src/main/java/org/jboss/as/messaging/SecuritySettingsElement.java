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

package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.hornetq.core.security.Role;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class SecuritySettingsElement extends AbstractModelElement<SecuritySettingsElement> {
    private static final long serialVersionUID = -35697785671908094L;
    private final String match;
    private final Set<Role> roles;

    public SecuritySettingsElement(final String match, final Set<Role> roles) {
        this.match = match;
        this.roles = roles;
    }

    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.MATCH.getLocalName(), match);
        Map<Attribute, String> rolesByType = new HashMap<Attribute, String>();

        for (Role role : roles) {
            String name = role.getName();
            if (role.isConsume()) {
                storeRoleToType(Attribute.CONSUME_NAME, name, rolesByType);
            }
            if (role.isCreateDurableQueue()) {
                storeRoleToType(Attribute.CREATEDURABLEQUEUE_NAME, name, rolesByType);
            }
            if (role.isCreateNonDurableQueue()) {
                storeRoleToType(Attribute.CREATE_NON_DURABLE_QUEUE_NAME, name, rolesByType);
            }
            if (role.isDeleteDurableQueue()) {
                storeRoleToType(Attribute.DELETEDURABLEQUEUE_NAME, name, rolesByType);
            }
            if (role.isDeleteNonDurableQueue()) {
                storeRoleToType(Attribute.DELETE_NON_DURABLE_QUEUE_NAME, name, rolesByType);
            }
            if (role.isManage()) {
                storeRoleToType(Attribute.MANAGE_NAME, name, rolesByType);
            }
            if (role.isSend()) {
                storeRoleToType(Attribute.SEND_NAME, name, rolesByType);
            }
        }

        for (Map.Entry<Attribute, String> entry : rolesByType.entrySet()) {
            streamWriter.writeStartElement(Element.PERMISSION_ELEMENT_NAME.getLocalName());
            streamWriter.writeAttribute(Attribute.TYPE_ATTR_NAME.getLocalName(), entry.getKey().getLocalName());
            streamWriter.writeAttribute(Attribute.ROLES_ATTR_NAME.getLocalName(), entry.getValue());
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    private void storeRoleToType(final Attribute type, final String role, final Map<Attribute, String> rolesByType) {
        String roleList = rolesByType.get(type);
        if (roleList == null) {
            roleList = role;
        } else {
            roleList += ", " + role;
        }
        rolesByType.put(type, roleList);
    }

    /** {@inheritDoc} */
    protected Class<SecuritySettingsElement> getElementClass() {
        return SecuritySettingsElement.class;
    }

    public String getMatch() {
        return match;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
