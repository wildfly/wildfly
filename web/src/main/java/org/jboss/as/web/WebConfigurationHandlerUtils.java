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

import static org.jboss.as.web.Constants.ACCESS_LOG;
import static org.jboss.as.web.Constants.CHECK_INTERVAL;
import static org.jboss.as.web.Constants.CONDITION;
import static org.jboss.as.web.Constants.CONTAINER;
import static org.jboss.as.web.Constants.CONTAINER_CONFIG;
import static org.jboss.as.web.Constants.DEVELOPMENT;
import static org.jboss.as.web.Constants.DIRECTORY;
import static org.jboss.as.web.Constants.DISABLED;
import static org.jboss.as.web.Constants.DISPLAY_SOURCE_FRAGMENT;
import static org.jboss.as.web.Constants.DUMP_SMAP;
import static org.jboss.as.web.Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE;
import static org.jboss.as.web.Constants.FILE_ENCODING;
import static org.jboss.as.web.Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS;
import static org.jboss.as.web.Constants.JAVA_ENCODING;
import static org.jboss.as.web.Constants.JSP_CONFIGURATION;
import static org.jboss.as.web.Constants.KEEP_GENERATED;
import static org.jboss.as.web.Constants.LISTINGS;
import static org.jboss.as.web.Constants.MAPPED_FILE;
import static org.jboss.as.web.Constants.MAX_DEPTH;
import static org.jboss.as.web.Constants.MIME_MAPPING;
import static org.jboss.as.web.Constants.MODIFICATION_TEST_INTERVAL;
import static org.jboss.as.web.Constants.READ_ONLY;
import static org.jboss.as.web.Constants.RECOMPILE_ON_FAIL;
import static org.jboss.as.web.Constants.REWRITE;
import static org.jboss.as.web.Constants.SCRATCH_DIR;
import static org.jboss.as.web.Constants.SENDFILE;
import static org.jboss.as.web.Constants.SMAP;
import static org.jboss.as.web.Constants.SOURCE_VM;
import static org.jboss.as.web.Constants.SSL;
import static org.jboss.as.web.Constants.SSO;
import static org.jboss.as.web.Constants.STATIC_RESOURCES;
import static org.jboss.as.web.Constants.TAG_POOLING;
import static org.jboss.as.web.Constants.TARGET_VM;
import static org.jboss.as.web.Constants.TRIM_SPACES;
import static org.jboss.as.web.Constants.WEBDAV;
import static org.jboss.as.web.Constants.WELCOME_FILE;
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


    /**

     subsystem=web

       - configuration=jsp
       - configuration=static
       - configuration=container

           - welcome files (list)

           - mime-mapping=$name

     */

    static final PathElement JSPPATH = PathElement.pathElement(CONTAINER_CONFIG, JSP_CONFIGURATION);
    static final PathElement RESOURCEPATH = PathElement.pathElement(CONTAINER_CONFIG, STATIC_RESOURCES);
    static final PathElement CONTAINERPATH = PathElement.pathElement(CONTAINER_CONFIG, Constants.CONTAINER);
    static final PathElement SSLPATH = PathElement.pathElement(SSL, "configuration");
    static final PathElement SSOPATH = PathElement.pathElement(SSO, "configuration");
    static final PathElement REWRITEPATH = PathElement.pathElement(REWRITE, "configuration");
    static final PathElement ACCESSLOG = PathElement.pathElement(ACCESS_LOG, "configuration");
    static final PathElement DIRECTORYPATH = PathElement.pathElement(DIRECTORY, "configuration");
    static final PathElement REWRITECOND = PathElement.pathElement(CONDITION, "configuration");

    /**
     * Initialize the configuration model, since add/remove operations would
     * not make sense. (the operation node should already have the default value set.
     *
     * @param resource the subsystem root resource
     * @param operation the subsystem add operation
     */

    static void initializeConfiguration(final Resource resource, final ModelNode operation) {
        // Create the child resources
        resource.registerChild(JSPPATH, Resource.Factory.create());
        resource.registerChild(RESOURCEPATH, Resource.Factory.create());
        resource.registerChild(CONTAINERPATH, Resource.Factory.create());

        final Resource jsp = resource.getChild(JSPPATH);
        final Resource resources = resource.getChild(RESOURCEPATH);
        final Resource container = resource.getChild(CONTAINERPATH);

        final ModelNode rootModel = resource.getModel();
        if (operation.hasDefined(Constants.DEFAULT_VIRTUAL_SERVER)) {
            rootModel.get(Constants.DEFAULT_VIRTUAL_SERVER).set(operation.get(Constants.DEFAULT_VIRTUAL_SERVER));
        }
        if (operation.hasDefined(Constants.NATIVE)) {
            rootModel.get(Constants.NATIVE).set(operation.get(Constants.NATIVE));
        }
        if (operation.hasDefined(Constants.INSTANCE_ID)) {
            rootModel.get(Constants.INSTANCE_ID).set(operation.get(Constants.INSTANCE_ID));
        }

        boolean hasJSP = false;
        boolean hasStatic = false;
        if (operation.hasDefined(CONTAINER_CONFIG)) {
            for(final String attribute :  operation.get(CONTAINER_CONFIG).keys()) {
                if (attribute.equals(JSP_CONFIGURATION) &&  operation.get(CONTAINER_CONFIG).hasDefined(JSP_CONFIGURATION)) {
                    hasJSP = true;
                    populateModel(jsp.getModel(), operation.get(CONTAINER_CONFIG, JSP_CONFIGURATION));
                } else if (attribute.equals(STATIC_RESOURCES) &&  operation.get(CONTAINER_CONFIG).hasDefined(STATIC_RESOURCES)) {
                    hasStatic = true;
                    populateModel(resources.getModel(), operation.get(CONTAINER_CONFIG, STATIC_RESOURCES));
                } else if (attribute.equals(MIME_MAPPING)) {
                    container.getModel().get(MIME_MAPPING).set(operation.get(CONTAINER_CONFIG, MIME_MAPPING));
                }  else if (attribute.equals(WELCOME_FILE)){
                    for(final ModelNode file : operation.get(CONTAINER_CONFIG, WELCOME_FILE).asList()) {
                        container.getModel().get(WELCOME_FILE).add(file.asString());
                    }
                } else if (attribute.equals(CONTAINER)) {
                    // the configuration=container case.
                    final ModelNode cont = operation.get(CONTAINER_CONFIG, Constants.CONTAINER);
                    if (cont.hasDefined(MIME_MAPPING)) {
                        container.getModel().get(MIME_MAPPING).set(cont.get(MIME_MAPPING));
                    }
                    if (cont.hasDefined(WELCOME_FILE)) {
                        for(final ModelNode file : cont.get(WELCOME_FILE).asList()) {
                            container.getModel().get(WELCOME_FILE).add(file.asString());
                        }
                    }
                }
            }
        }
        if (!hasJSP) {
            // we don't have JSP but we hack the default values here.
            jsp.getModel().set(DefaultJspConfig.getDefaultStaticResource());
        }
        if (!hasStatic) {
            // we don't have static-resources but we hack the default values here.
            resources.getModel().set(DefaultStaticResources.getDefaultStaticResource());
        }
    }

    /* create the sso=configuration, the rewrite=rule-n (if defined) and the access-log=configuration/directory=configuration */
    static void initializeHost(final Resource resource, final ModelNode operation) {

       for(final String attribute :  operation.keys()) {
            if (attribute.equals(REWRITE) && operation.get(REWRITE).isDefined()) {
                populateReWrite(operation, resource);
            } else if (attribute.equals(ACCESS_LOG) && operation.get(ACCESS_LOG).isDefined())  {
                resource.registerChild(ACCESSLOG, Resource.Factory.create());
                final Resource accesslog = resource.getChild(ACCESSLOG);
                accesslog.registerChild(DIRECTORYPATH, Resource.Factory.create());
                final Resource directory = accesslog.getChild(DIRECTORYPATH);
                if (operation.get(ACCESS_LOG).hasDefined("configuration"))
                    populateAccessLog(accesslog.getModel(), operation.get(ACCESS_LOG).get("configuration"), directory);
                else
                    populateAccessLog(accesslog.getModel(), operation.get(ACCESS_LOG), directory);
             } else if (attribute.equals(SSO) && operation.get(SSO).isDefined())  {
                 resource.registerChild(SSOPATH, Resource.Factory.create());
                 final Resource sso = resource.getChild(SSOPATH);
                 if (operation.get(SSO).hasDefined("configuration"))
                     populateModel(sso.getModel(), operation.get(SSO).get("configuration"));
                 else
                     populateModel(sso.getModel(), operation.get(SSO));
             }
        }
    }
    static void populateReWrite(final ModelNode operation, final Resource resource) {
        int num = 0;

        for(final ModelNode entry : operation.get(REWRITE).asList()) {
            String name = "rule-" + num++;
            PathElement rewritepath = PathElement.pathElement(REWRITE, name);
            resource.registerChild(rewritepath, Resource.Factory.create());
            final Resource rewritevalve = resource.getChild(rewritepath);

            ModelNode rule;
            if (entry.hasDefined(name))
                rule = entry.get(name);
            else
                rule = entry;

            for(final String attribute : rule.keys()) {
                if(rule.hasDefined(attribute)) {
                    if (attribute.equals(CONDITION)) {
                        // Create condition-n list.
                        int j = 0;
                        for(final ModelNode cond : rule.get(CONDITION).asList()) {
                            String condname = "condition-" + j++;
                            PathElement conditionpath = PathElement.pathElement(CONDITION, condname);
                            rewritevalve.registerChild(conditionpath, Resource.Factory.create());
                            final Resource condition = rewritevalve.getChild(conditionpath);
                            if (cond.hasDefined(condname))
                                populateModel(condition.getModel(),  cond.get(condname));
                            else
                                populateModel(condition.getModel(),  cond);

                        }
                    } else {
                        rewritevalve.getModel().get(attribute).set(rule.get(attribute));
                   }
                }
            }
        }
    }

    static void populateAccessLog(final ModelNode subModel, final ModelNode operation, final Resource directory) {
         for(final String attribute : operation.keys()) {
            if(operation.hasDefined(attribute)) {
                if (attribute.equals(DIRECTORY)) {
                    if (operation.get(DIRECTORY).isDefined() && operation.get(DIRECTORY).has("configuration"))
                        populateModel(directory.getModel(), operation.get(DIRECTORY).get("configuration"));
                    else
                        populateModel(directory.getModel(), operation.get(DIRECTORY));
                } else
                    subModel.get(attribute).set(operation.get(attribute));
            }
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
        jsp.registerReadWriteAttribute(TRIM_SPACES, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(TAG_POOLING, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(MAPPED_FILE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(CHECK_INTERVAL, null, new WriteAttributeHandlers.IntRangeValidatingHandler(-1), Storage.CONFIGURATION);
        jsp.registerReadWriteAttribute(MODIFICATION_TEST_INTERVAL, null, new WriteAttributeHandlers.IntRangeValidatingHandler(-1), Storage.CONFIGURATION);
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

    static void initResourcesAttributes(final ManagementResourceRegistration resources) {
        resources.registerReadWriteAttribute(LISTINGS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(SENDFILE, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(FILE_ENCODING, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(READ_ONLY, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(WEBDAV, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(MAX_DEPTH, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        resources.registerReadWriteAttribute(DISABLED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
    }

    public static void initializeConnector(Resource resource, ModelNode model) {
        if (model.hasDefined(SSL)) {
            resource.registerChild(SSLPATH, Resource.Factory.create());
            final Resource ssl = resource.getChild(SSLPATH);
            if (model.get(SSL).hasDefined("configuration"))
                populateModel(ssl.getModel(), model.get(SSL).get("configuration"));
            else
                populateModel(ssl.getModel(), model.get(SSL));
        }
    }
}
