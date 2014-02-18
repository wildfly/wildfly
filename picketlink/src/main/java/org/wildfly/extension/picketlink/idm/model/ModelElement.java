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

package org.wildfly.extension.picketlink.idm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p> {@link Enum} class where all model elements name (attributes and elements) are defined. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 8, 2012
 */
public enum ModelElement {
    /*
     * Common elements shared by all resources definitions
     */
    COMMON_URL("url"),
    COMMON_NAME("name"),
    COMMON_VALUE("value"),
    COMMON_CLASS_NAME("class-name"),
    COMMON_CODE("code"),
    COMMON_SUPPORTS_ALL("supports-all"),
    COMMON_MODULE("module"),
    /*
     * Identity Management model elements
     */
    PARTITION_MANAGER("partition-manager"),
    IDENTITY_CONFIGURATION("identity-configuration"),
    IDENTITY_STORE_CREDENTIAL_HANDLER("credential-handler"),
    IDENTITY_STORE_SUPPORT_ATTRIBUTE("support-attribute"),
    IDENTITY_STORE_SUPPORT_CREDENTIAL("support-credential"),
    IDENTITY_MANAGEMENT_JNDI_NAME("jndi-name"),
    JPA_STORE("jpa-store"),
    JPA_STORE_DATASOURCE("data-source"),
    JPA_STORE_ENTITY_MODULE("entity-module"),
    JPA_STORE_ENTITY_MODULE_UNIT_NAME("entity-module-unit-name"),
    JPA_STORE_ENTITY_MANAGER_FACTORY("entity-manager-factory"),
    FILE_STORE("file-store"),
    FILE_STORE_WORKING_DIR("working-dir"),
    FILE_STORE_RELATIVE_TO("relative-to"),
    FILE_STORE_ALWAYS_CREATE_FILE("always-create-files"),
    FILE_STORE_ASYNC_WRITE("async-write"),
    FILE_STORE_ASYNC_THREAD_POOL("async-write-thread-pool"),
    LDAP_STORE("ldap-store"),
    LDAP_STORE_URL(COMMON_URL.getName()),
    LDAP_STORE_BIND_DN("bind-dn"),
    LDAP_STORE_BIND_CREDENTIAL("bind-credential"),
    LDAP_STORE_BASE_DN_SUFFIX("base-dn-suffix"),
    LDAP_STORE_MAPPING("mapping"),
    LDAP_STORE_MAPPING_BASE_DN(LDAP_STORE_BASE_DN_SUFFIX.getName()),
    LDAP_STORE_MAPPING_OBJECT_CLASSES("object-classes"),
    LDAP_STORE_MAPPING_PARENT_ATTRIBUTE_NAME("parent-membership-attribute-name"),
    LDAP_STORE_MAPPING_RELATES_TO("relates-to"),
    LDAP_STORE_ATTRIBUTE("attribute"),
    LDAP_STORE_ATTRIBUTE_NAME(COMMON_NAME.getName()),
    LDAP_STORE_ATTRIBUTE_LDAP_NAME("ldap-name"),
    LDAP_STORE_ATTRIBUTE_IS_IDENTIFIER("is-identifier"),
    LDAP_STORE_ATTRIBUTE_READ_ONLY("read-only"),
    SUPPORTED_TYPES("supported-types"),
    SUPPORTED_TYPE("supported-type");
    private static final Map<String, ModelElement> modelElements = new HashMap<String, ModelElement>();

    static {
        for (ModelElement element : values()) {
            modelElements.put(element.getName(), element);
        }
    }

    private final String name;

    private ModelElement(String name) {
        this.name = name;
    }

    public static ModelElement forName(String name) {
        return modelElements.get(name);
    }

    public String getName() {
        return this.name;
    }
}
