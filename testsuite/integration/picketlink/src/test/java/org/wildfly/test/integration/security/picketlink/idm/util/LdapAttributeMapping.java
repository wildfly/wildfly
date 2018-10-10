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

package org.wildfly.test.integration.security.picketlink.idm.util;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Pedro Igor
 */
public class LdapAttributeMapping {

    private final String name;
    private final String ldapName;
    private final boolean identifier;
    private final boolean readonly;

    public LdapAttributeMapping(String name, String ldapName, boolean identifier, boolean readonly) {
        this.name = name;
        this.ldapName = ldapName;
        this.identifier = identifier;
        this.readonly = readonly;
    }

    public ModelNode createAddOperation(ModelNode parentNode) {
        ModelNode attributeAddOperation = Util.createAddOperation(PathAddress.pathAddress(parentNode.get(OP_ADDR)).append("attribute", this.name));

        attributeAddOperation.get("name").set(this.name);
        attributeAddOperation.get("ldap-name").set(this.ldapName);
        attributeAddOperation.get("is-identifier").set(this.identifier);
        attributeAddOperation.get("read-only").set(this.readonly);

        return attributeAddOperation;
    }
}
