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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_REPLACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentReplaceHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.server.deployment.DeploymentUploadURLHandler;
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

    public static final ModelNode getDeploymentDescription(Locale locale) {
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
        root.get(ATTRIBUTES, HASH, TYPE).set(ModelType.BYTES);
        root.get(ATTRIBUTES, HASH, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(ATTRIBUTES, HASH, REQUIRED).set(true);
        root.get(ATTRIBUTES, HASH, MIN_LENGTH).set(20);
        root.get(ATTRIBUTES, HASH, MAX_LENGTH).set(20);
        root.get(ATTRIBUTES, HASH, NILLABLE).set(false);
        root.get(ATTRIBUTES, START, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, START, DESCRIPTION).set(bundle.getString("deployment.start"));
        root.get(ATTRIBUTES, START, REQUIRED).set(true);
        root.get(OPERATIONS);
        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static final ModelNode getUploadDeploymentBytesOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentUploadBytesHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.upload-bytes"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(true);
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
        root.get(OPERATION_NAME).set(DeploymentUploadURLHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.upload-url"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(true);
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

    public static final ModelNode getAddDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("add-deployment");
        root.get(DESCRIPTION).set(bundle.getString("deployment.add"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, DESCRIPTION).set(bundle.getString("deployment.runtime-name"));
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, RUNTIME_NAME, NILLABLE).set(true);
        root.get(REQUEST_PROPERTIES, HASH, TYPE).set(ModelType.BYTES);
        root.get(REQUEST_PROPERTIES, HASH, DESCRIPTION).set(bundle.getString("deployment.hash"));
        root.get(REQUEST_PROPERTIES, HASH, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, HASH, MIN_LENGTH).set(20);
        root.get(REQUEST_PROPERTIES, HASH, NILLABLE).set(false);
//        root.get(REQUEST_PROPERTIES, START, TYPE).set(ModelType.BOOLEAN);
//        root.get(REQUEST_PROPERTIES, START, DESCRIPTION).set(bundle.getString("deployment.start"));
//        root.get(REQUEST_PROPERTIES, START, REQUIRED).set(false);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getDeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentDeployHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.deploy"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getReplaceDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentReplaceHandler.OPERATION_NAME);
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
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getFullReplaceDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentFullReplaceHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.full-replace"));
        root.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("deployment.name"));
        root.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, NAME, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getUndeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentUndeployHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.undeploy"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getRedeployDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentRedeployHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.redeploy"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static final ModelNode getRemoveDeploymentOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DeploymentRemoveHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("deployment.remove"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(String[] args) {
        System.out.println(getAddDeploymentOperation(null));
        System.out.println(getDeployDeploymentOperation(null));
        System.out.println(getDeploymentDescription(null));
        System.out.println(getFullReplaceDeploymentOperation(null));
        System.out.println(getRedeployDeploymentOperation(null));
        System.out.println(getRemoveDeploymentOperation(null));
        System.out.println(getReplaceDeploymentOperation(null));
        System.out.println(getUndeployDeploymentOperation(null));
        System.out.println(getUploadDeploymentBytesOperation(null));
        System.out.println(getUploadDeploymentURLOperation(null));
    }
}
