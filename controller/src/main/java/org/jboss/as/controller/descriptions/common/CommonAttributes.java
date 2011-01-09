/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 */package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Some bits and pieces of model attribute description that are used across models.
 *
 * @author Brian Stansberry
 */
public class CommonAttributes {

    private static final ModelNode NAMESPACE_PREFIX_ATTRIBUTE = new ModelNode();
    private static final ModelNode SCHEMA_LOCATION_ATTRIBUTE = new ModelNode();

    static {
        NAMESPACE_PREFIX_ATTRIBUTE.get(NAME).set("namespaces");
        NAMESPACE_PREFIX_ATTRIBUTE.get(TYPE).set(ModelType.OBJECT);
        NAMESPACE_PREFIX_ATTRIBUTE.get(VALUE_TYPE).set(ModelType.STRING);
        NAMESPACE_PREFIX_ATTRIBUTE.get(DESCRIPTION).set("Map of namespaces used in the configuration XML document, where keys are namespace prefixes and values are schema URIs.");
        NAMESPACE_PREFIX_ATTRIBUTE.get(REQUIRED).set(false);

        SCHEMA_LOCATION_ATTRIBUTE.get(NAME).set("schema-locations");
        SCHEMA_LOCATION_ATTRIBUTE.get(TYPE).set(ModelType.OBJECT);
        SCHEMA_LOCATION_ATTRIBUTE.get(VALUE_TYPE).set(ModelType.STRING);
        SCHEMA_LOCATION_ATTRIBUTE.get(DESCRIPTION).set("Map of locations of XML schemas used in the configuration XML document, where keys are schema URIs and values are locations where the schema can be found.");
        SCHEMA_LOCATION_ATTRIBUTE.get(REQUIRED).set(false);
    }

    public static ModelNode getNamespacePrefixAttribute() {
        return NAMESPACE_PREFIX_ATTRIBUTE.clone();
    }

    public static ModelNode getSchemaLocationAttribute() {
        return SCHEMA_LOCATION_ATTRIBUTE.clone();
    }

}
