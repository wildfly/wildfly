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

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_CODE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_BASE_DN_SUFFIX;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING_OBJECT_CLASSES;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING_RELATES_TO;

/**
 * @author Pedro Igor
 */
public class LdapMapping {

    private final String[] objectClass;
    private final String type;
    private final String baseDn;
    private final List<LdapAttributeMapping> attributes = new ArrayList<LdapAttributeMapping>();
    private String relatesTo;

    public LdapMapping(String type, String relatesTo) {
        this(type, null, null);
        this.relatesTo = relatesTo;
    }

    public LdapMapping(String type, String baseDn, String... objectClass) {
        this.type = type;
        this.baseDn = baseDn;
        this.objectClass = objectClass;
    }

    public void addAttribute(String name, String ldapName, boolean identifier, boolean readonly) {
        this.attributes.add(new LdapAttributeMapping(name, ldapName, identifier, readonly));
    }

    public ModelNode createAddOperation(ModelNode parentNode) {
        final ModelNode compositeOp = new ModelNode();

        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        ModelNode mappingAddOperation = Util.createAddOperation(PathAddress.pathAddress(parentNode.get(OP_ADDR)).append(LDAP_STORE_MAPPING
                                                                                                                        .getName(), this.type));

        mappingAddOperation.get(COMMON_CODE.getName()).set(this.type);

        if (this.baseDn != null) {
            mappingAddOperation.get(LDAP_STORE_BASE_DN_SUFFIX.getName()).set(this.baseDn);
        }

        if (this.objectClass != null) {
            StringBuilder objectClassesBuilder = new StringBuilder();

            for (String objectClass : this.objectClass) {
                if (objectClassesBuilder.length() > 0) {
                    objectClassesBuilder.append(",");
                }

                objectClassesBuilder.append(objectClass);
            }

            mappingAddOperation.get(LDAP_STORE_MAPPING_OBJECT_CLASSES.getName()).set(objectClassesBuilder.toString());
        }

        if (this.relatesTo != null) {
            mappingAddOperation.get(LDAP_STORE_MAPPING_RELATES_TO.getName()).set(this.relatesTo);
        }

        steps.add(mappingAddOperation);

        for (LdapAttributeMapping attribute : this.attributes) {
            steps.add(attribute.createAddOperation(mappingAddOperation));
        }

        return compositeOp;
    }
}
