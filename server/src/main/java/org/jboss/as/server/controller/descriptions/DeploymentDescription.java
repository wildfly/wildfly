/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATUS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for deployment resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentDescription {

    private static final String RESOURCE_NAME = DeploymentDescription.class.getPackage().getName() + ".LocalDescriptions";

    private DeploymentDescription() {
    }

    public static final ModelNode getDeploymentDescription(Locale locale, boolean includeEnabled, boolean includeContent, boolean includeRuntime) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("deployment"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
        root.get(ATTRIBUTES, NAME, NILLABLE).set(false);
        root.get(ATTRIBUTES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(ATTRIBUTES, RUNTIME_NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(ATTRIBUTES, RUNTIME_NAME, NILLABLE).set(false);
        if (includeContent) {
            root.get(ATTRIBUTES, CONTENT, TYPE).set(ModelType.LIST);
            root.get(ATTRIBUTES, CONTENT, DESCRIPTION).set(bundle.getString("deployment.content"));
            root.get(ATTRIBUTES, CONTENT, REQUIRED).set(true);
            root.get(ATTRIBUTES, CONTENT, MIN_LENGTH).set(1);

            //Not all of these should be here when the model is read

            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, TYPE).set(ModelType.INT);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, DESCRIPTION).set(bundle.getString("deployment.inputstream"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, MIN).set(0);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, NILLABLE).set(true);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, TYPE).set(ModelType.BYTES);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, DESCRIPTION).set(bundle.getString("deployment.hash"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, MIN_LENGTH).set(20);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, MAX_LENGTH).set(20);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, HASH, NILLABLE).set(true);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, BYTES, TYPE).set(ModelType.BYTES);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, BYTES, DESCRIPTION).set(bundle.getString("deployment.bytes"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, BYTES, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, BYTES, MIN_LENGTH).set(1);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, BYTES, NILLABLE).set(true);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, URL, TYPE).set(ModelType.STRING);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, URL, DESCRIPTION).set(bundle.getString("deployment.url"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, URL, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, URL, MIN_LENGTH).set(1);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, URL, NILLABLE).set(true);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, PATH, TYPE).set(ModelType.STRING);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, PATH, DESCRIPTION).set(bundle.getString("deployment.path"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, PATH, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, PATH, MIN_LENGTH).set(1);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, PATH, NILLABLE).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, RELATIVE_TO, TYPE).set(ModelType.STRING);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, RELATIVE_TO, DESCRIPTION).set(bundle.getString("deployment.relative-to"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, RELATIVE_TO, REQUIRED).set(false);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, RELATIVE_TO, MIN_LENGTH).set(1);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, RELATIVE_TO, NILLABLE).set(true);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, ARCHIVE, TYPE).set(ModelType.BOOLEAN);
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, ARCHIVE, DESCRIPTION).set(bundle.getString("deployment.archive"));
            root.get(ATTRIBUTES, CONTENT, VALUE_TYPE, ARCHIVE, REQUIRED).set(false);
        }
        if (includeEnabled) {
            root.get(ATTRIBUTES, ENABLED, TYPE).set(ModelType.BOOLEAN);
            root.get(ATTRIBUTES, ENABLED, DESCRIPTION).set(bundle.getString("deployment.enabled"));
            root.get(ATTRIBUTES, ENABLED, REQUIRED).set(true);
            if (includeContent) {
                // includeEnabled && includeContent means this is for a server
                root.get(ATTRIBUTES, PERSISTENT, TYPE).set(ModelType.BOOLEAN);
                root.get(ATTRIBUTES, PERSISTENT, DESCRIPTION).set(bundle.getString("deployment.persistent"));
                root.get(ATTRIBUTES, PERSISTENT, REQUIRED).set(true);

                root.get(ATTRIBUTES, STATUS, TYPE).set(ModelType.STRING);
                root.get(ATTRIBUTES, STATUS, DESCRIPTION).set(bundle.getString("deployment.status"));
                root.get(ATTRIBUTES, STATUS, REQUIRED).set(false);
            }
        }

        root.get(OPERATIONS);  // placeholder

        if (includeRuntime) {
            root.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set(bundle.getString("deployment.subsystem"));
            root.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(0);
            root.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);

            root.get(CHILDREN, SUBDEPLOYMENT, DESCRIPTION).set(bundle.getString("deployment.subdeployment"));
            root.get(CHILDREN, SUBDEPLOYMENT, MIN_OCCURS).set(0);
            root.get(CHILDREN, SUBDEPLOYMENT, MODEL_DESCRIPTION);
        } else {
            root.get(CHILDREN).setEmptyObject();
        }

        return root;
    }

    public static ModelNode getSubDeploymentDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("deployment.subdeployment"));

        root.get(ATTRIBUTES).setEmptyObject();
        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set(bundle.getString("deployment.subsystem"));
        root.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(0);
        root.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);

        return root;
    }

    public static final ModelNode getUploadDeploymentBytesOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(UPLOAD_DEPLOYMENT_BYTES);
        root.get(DESCRIPTION).set(bundle.getString("deployment.upload-bytes"));
        root.get(REQUEST_PROPERTIES, BYTES, TYPE).set(ModelType.BYTES);
        root.get(REQUEST_PROPERTIES, BYTES, DESCRIPTION).set(bundle.getString("deployment.bytes"));
        root.get(REQUEST_PROPERTIES, BYTES, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, BYTES, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, BYTES, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.BYTES);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(REPLY_PROPERTIES, MIN_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, MAX_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, NILLABLE).set(false);
        return root;
    }

    public static final ModelNode getUploadDeploymentURLOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(UPLOAD_DEPLOYMENT_URL);
        root.get(DESCRIPTION).set(bundle.getString("deployment.upload-url"));
        root.get(REQUEST_PROPERTIES, URL, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, URL, DESCRIPTION).set(bundle.getString("deployment.url"));
        root.get(REQUEST_PROPERTIES, URL, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, URL, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, URL, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.BYTES);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(REPLY_PROPERTIES, MIN_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, MAX_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, NILLABLE).set(false);
        return root;
    }

    public static final ModelNode getUploadDeploymentStreamAttachmentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(UPLOAD_DEPLOYMENT_STREAM);
        root.get(DESCRIPTION).set(bundle.getString("deployment.upload-stream"));
        root.get(REQUEST_PROPERTIES, INPUT_STREAM_INDEX, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, INPUT_STREAM_INDEX, DESCRIPTION).set(bundle.getString("deployment.inputstream"));
        root.get(REQUEST_PROPERTIES, INPUT_STREAM_INDEX, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, INPUT_STREAM_INDEX, MIN).set(0);
        root.get(REQUEST_PROPERTIES, INPUT_STREAM_INDEX, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.BYTES);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(REPLY_PROPERTIES, MIN_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, MAX_LENGTH).set(20);
        root.get(REPLY_PROPERTIES, NILLABLE).set(false);
        return root;
    }

    public static final ModelNode getAddDeploymentOperation(Locale locale, boolean includeEnabled) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("deployment.add"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(true);
        getDeploymentContentParamDescription(root, bundle);
        if (includeEnabled) {
            root.get(REQUEST_PROPERTIES, ENABLED, TYPE).set(ModelType.BOOLEAN);
            root.get(REQUEST_PROPERTIES, ENABLED, DESCRIPTION).set(bundle.getString("deployment.enabled"));
            root.get(REQUEST_PROPERTIES, ENABLED, REQUIRED).set(false);
        }
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getDeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DEPLOY);
        root.get(DESCRIPTION).set(bundle.getString("deployment.deploy"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getReplaceDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REPLACE_DEPLOYMENT);
        root.get(DESCRIPTION).set(bundle.getString("deployment.replace"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.replace.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, TO_REPLACE, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, TO_REPLACE, DESCRIPTION).set(bundle.getString("deployment.replace.to-replace"));
        root.get(REQUEST_PROPERTIES, TO_REPLACE, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, TO_REPLACE, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, TO_REPLACE, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getFullReplaceDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(FULL_REPLACE_DEPLOYMENT);
        root.get(DESCRIPTION).set(bundle.getString("deployment.full-replace"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(false);
        getDeploymentContentParamDescription(root, bundle);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getUndeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(UNDEPLOY);
        root.get(DESCRIPTION).set(bundle.getString("deployment.undeploy"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getRedeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REDEPLOY);
        root.get(DESCRIPTION).set(bundle.getString("deployment.redeploy"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    private static void getDeploymentContentParamDescription(ModelNode root, ResourceBundle bundle) {
        root.get(REQUEST_PROPERTIES, CONTENT, TYPE).set(ModelType.LIST);
        root.get(REQUEST_PROPERTIES, CONTENT, DESCRIPTION).set(bundle.getString("deployment.content"));
        root.get(REQUEST_PROPERTIES, CONTENT, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, TYPE).set(ModelType.INT);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, DESCRIPTION).set(bundle.getString("deployment.inputstream"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, MIN).set(0);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, INPUT_STREAM_INDEX, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, TYPE).set(ModelType.BYTES);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, MIN_LENGTH).set(20);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, MAX_LENGTH).set(20);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, HASH, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, BYTES, TYPE).set(ModelType.BYTES);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, BYTES, DESCRIPTION).set(bundle.getString("deployment.bytes"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, BYTES, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, BYTES, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, BYTES, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, URL, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, URL, DESCRIPTION).set(bundle.getString("deployment.url"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, URL, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, URL, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, URL, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, PATH, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, PATH, DESCRIPTION).set(bundle.getString("deployment.path"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, PATH, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, PATH, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, PATH, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, RELATIVE_TO, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, RELATIVE_TO, DESCRIPTION).set(bundle.getString("deployment.relative-to"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, RELATIVE_TO, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, RELATIVE_TO, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, RELATIVE_TO, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, ARCHIVE, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, ARCHIVE, DESCRIPTION).set(bundle.getString("deployment.archive"));
        root.get(REQUEST_PROPERTIES, CONTENT, VALUE_TYPE, ARCHIVE, REQUIRED).set(false);
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(String[] args) {
        System.out.println(getDeploymentDescription(null, true, true, false));
        System.out.println(getAddDeploymentOperation(null, true));
        System.out.println(getDeployDeploymentOperation(null));
        System.out.println(getFullReplaceDeploymentOperation(null));
        System.out.println(getRedeployDeploymentOperation(null));
        System.out.println(getReplaceDeploymentOperation(null));
        System.out.println(getUndeployDeploymentOperation(null));
        System.out.println(getUploadDeploymentBytesOperation(null));
        System.out.println(getUploadDeploymentURLOperation(null));
    }
}
