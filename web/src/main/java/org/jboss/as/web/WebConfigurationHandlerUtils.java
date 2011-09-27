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
 */

package org.jboss.as.web;

import static org.jboss.as.web.Constants.CHECK_INTERVAL;
import static org.jboss.as.web.Constants.CONTAINER_CONFIG;
import static org.jboss.as.web.Constants.DEVELOPMENT;
import static org.jboss.as.web.Constants.DISABLED;
import static org.jboss.as.web.Constants.DISPLAY_SOURCE_FRAGMENT;
import static org.jboss.as.web.Constants.DUMP_SMAP;
import static org.jboss.as.web.Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE;
import static org.jboss.as.web.Constants.FILE_ENCONDING;
import static org.jboss.as.web.Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS;
import static org.jboss.as.web.Constants.JAVA_ENCODING;
import static org.jboss.as.web.Constants.JSP_CONFIGURATION;
import static org.jboss.as.web.Constants.KEEP_GENERATED;
import static org.jboss.as.web.Constants.LISTINGS;
import static org.jboss.as.web.Constants.MAPPED_FILE;
import static org.jboss.as.web.Constants.MAX_DEPTH;
import static org.jboss.as.web.Constants.MODIFIFICATION_TEST_INTERVAL;
import static org.jboss.as.web.Constants.READ_ONLY;
import static org.jboss.as.web.Constants.RECOMPILE_ON_FAIL;
import static org.jboss.as.web.Constants.SCRATCH_DIR;
import static org.jboss.as.web.Constants.SECRET;
import static org.jboss.as.web.Constants.SENDFILE;
import static org.jboss.as.web.Constants.SMAP;
import static org.jboss.as.web.Constants.SOURCE_VM;
import static org.jboss.as.web.Constants.STATIC_RESOURCES;
import static org.jboss.as.web.Constants.TAG_POOLING;
import static org.jboss.as.web.Constants.TARGET_VM;
import static org.jboss.as.web.Constants.TRIM_SPACES;
import static org.jboss.as.web.Constants.WEBDAV;
import static org.jboss.as.web.Constants.X_POWERED_BY;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Utils for the web container configuration.
 *
 * @author Emanuel Muckenhuber
 */
class WebConfigurationHandlerUtils {

    static final PathElement JSP = PathElement.pathElement(CONTAINER_CONFIG, JSP_CONFIGURATION);
    static final PathElement RESOURCE = PathElement.pathElement(CONTAINER_CONFIG, STATIC_RESOURCES);

    static final String[] RESOURCE_CONFIGURATION_ATTRIBUTES = new String[] { LISTINGS, SENDFILE, FILE_ENCONDING, READ_ONLY, WEBDAV, SECRET, MAX_DEPTH, DISABLED };
    static final String[] JSP_CONFIGURATION_ATTRIBUTES = new String[] { DEVELOPMENT, DISABLED, KEEP_GENERATED, TRIM_SPACES, TAG_POOLING, MAPPED_FILE, CHECK_INTERVAL,
                            MODIFIFICATION_TEST_INTERVAL, RECOMPILE_ON_FAIL, SMAP, DUMP_SMAP, GENERATE_STRINGS_AS_CHAR_ARRAYS, SCRATCH_DIR, SOURCE_VM, TARGET_VM,
                            JAVA_ENCODING, X_POWERED_BY, DISPLAY_SOURCE_FRAGMENT };

    /**
     * Initialize the configuration model, since add/remove operations would
     * not make sense. (the operation node should already have the default value set.
     *
     * @param context the operation context
     */

    static void initializeConfiguration(final Resource resource, final ModelNode operation) {
        // Create the child resources
        resource.registerChild(JSP, Resource.Factory.create());
        resource.registerChild(RESOURCE, Resource.Factory.create());

        final Resource jsp = resource.getChild(JSP);
        final Resource resources = resource.getChild(RESOURCE);

        System.out.println("initializeConfiguration: " + operation.get(CONTAINER_CONFIG, JSP_CONFIGURATION).keys());

        if(operation.hasDefined(CONTAINER_CONFIG) && operation.get(CONTAINER_CONFIG).hasDefined(JSP_CONFIGURATION)) {
            populateModel(jsp.getModel(), operation.get(CONTAINER_CONFIG, JSP_CONFIGURATION));
        }

        if(operation.hasDefined(CONTAINER_CONFIG) && operation.get(CONTAINER_CONFIG).hasDefined(STATIC_RESOURCES)) {
            populateModel(resources.getModel(), operation.get(CONTAINER_CONFIG, STATIC_RESOURCES));
        }
    }


    static void populateModel(final ModelNode subModel, final ModelNode operation) {
        for(final String attribute : operation.keys()) {
            if(operation.hasDefined(attribute)) {
                subModel.get(attribute).set(operation.get(attribute));
            }
        }
    }

    static void initJSPAttributes(final ManagementResourceRegistration jsp) {
        jsp.registerReadWriteAttribute(DEVELOPMENT, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(DISABLED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(KEEP_GENERATED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(TAG_POOLING, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(MAPPED_FILE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(CHECK_INTERVAL, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(MODIFIFICATION_TEST_INTERVAL, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(RECOMPILE_ON_FAIL, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(SMAP, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(DUMP_SMAP, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(GENERATE_STRINGS_AS_CHAR_ARRAYS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(SCRATCH_DIR, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(SOURCE_VM, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(TARGET_VM, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(JAVA_ENCODING, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(X_POWERED_BY, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(DISPLAY_SOURCE_FRAGMENT, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
    }

    static void initResourcesAttribtues(final ManagementResourceRegistration resources) {
        resources.registerReadWriteAttribute(LISTINGS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(SENDFILE, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(FILE_ENCONDING, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(READ_ONLY, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(WEBDAV, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(MAX_DEPTH, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(DISABLED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
    }
}
