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

package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import static org.jboss.as.server.deployment.scanner.CommonAttributes.*;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Descriptions of the deployment scanner subsystem resources and operations.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 */
public class DeploymentSubsystemDescriptions {

    static final String RESOURCE_NAME = DeploymentSubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelNode getSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("deployment.scanner.subsystem"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

        subsystem.get(ATTRIBUTES).setEmptyObject();
        subsystem.get(OPERATIONS);
        subsystem.get(CHILDREN, CommonAttributes.SCANNER, DESCRIPTION).set(bundle.getString("deployment.scanner.subsystem.scanners"));

        return subsystem;
    }

    static final ModelNode getSubsystemAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();

        operation.get(OPERATION_NAME).set(ADD);
        operation.get(DESCRIPTION).set(bundle.getString("deployment.scanner.subsystem.add"));
        operation.get(REQUEST_PROPERTIES).setEmptyObject();
        operation.get(REPLY_PROPERTIES).setEmptyObject();

        return operation;
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();

        operation.get(OPERATION_NAME).set(REMOVE);
        operation.get(DESCRIPTION).set(bundle.getString("deployment.scanner.subsystem.remove"));
        operation.get(REQUEST_PROPERTIES).setEmptyObject();
        operation.get(REPLY_PROPERTIES).setEmptyObject();

        return operation;
    }

    static final ModelNode getScannerDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("scanner"));
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);

        root.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("scanner.name"));
        root.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        root.get(ATTRIBUTES, PATH, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, PATH, DESCRIPTION).set(bundle.getString("scanner.path"));
        root.get(ATTRIBUTES, PATH, REQUIRED).set(true);
        root.get(ATTRIBUTES, PATH, MIN_LENGTH).set(1);
        root.get(ATTRIBUTES, RELATIVE_TO, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("scanner.relative-to"));
        root.get(ATTRIBUTES, RELATIVE_TO, REQUIRED).set(false);
        root.get(ATTRIBUTES, SCAN_ENABLED, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, SCAN_ENABLED, DESCRIPTION).set(bundle.getString("scanner.enabled"));
        root.get(ATTRIBUTES, SCAN_ENABLED, REQUIRED).set(false);
        root.get(ATTRIBUTES, SCAN_ENABLED, DEFAULT).set(true);
        root.get(ATTRIBUTES, SCAN_INTERVAL, TYPE).set(ModelType.INT);
        root.get(ATTRIBUTES, SCAN_INTERVAL, DESCRIPTION).set(bundle.getString("scanner.interval"));
        root.get(ATTRIBUTES, SCAN_INTERVAL, REQUIRED).set(false);
        root.get(ATTRIBUTES, SCAN_INTERVAL, DEFAULT).set(5000);
        root.get(ATTRIBUTES, AUTO_DEPLOY_ZIPPED, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, AUTO_DEPLOY_ZIPPED, DESCRIPTION).set(bundle.getString("scanner.auto.deploy.zipped"));
        root.get(ATTRIBUTES, AUTO_DEPLOY_ZIPPED, REQUIRED).set(false);
        root.get(ATTRIBUTES, AUTO_DEPLOY_ZIPPED, DEFAULT).set(true);
        root.get(ATTRIBUTES, AUTO_DEPLOY_EXPLODED, TYPE).set(ModelType.BOOLEAN);
        root.get(ATTRIBUTES, AUTO_DEPLOY_EXPLODED, DESCRIPTION).set(bundle.getString("scanner.auto.deploy.exploded"));
        root.get(ATTRIBUTES, AUTO_DEPLOY_EXPLODED, REQUIRED).set(false);
        root.get(ATTRIBUTES, AUTO_DEPLOY_EXPLODED, DEFAULT).set(false);
        root.get(ATTRIBUTES, DEPLOYMENT_TIMEOUT, TYPE).set(ModelType.LONG);
        root.get(ATTRIBUTES, DEPLOYMENT_TIMEOUT, DESCRIPTION).set(bundle.getString("scanner.deployment.timeout"));
        root.get(ATTRIBUTES, DEPLOYMENT_TIMEOUT, REQUIRED).set(false);
        root.get(ATTRIBUTES, DEPLOYMENT_TIMEOUT, DEFAULT).set(60L);

        root.get(OPERATIONS);

        root.get(CHILDREN);

        return root;
    }

    static final ModelNode getScannerAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();

        operation.get(OPERATION_NAME).set(ADD);
        operation.get(DESCRIPTION).set(bundle.getString("scanner.add"));
        operation.get(REQUEST_PROPERTIES, PATH, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, PATH, DESCRIPTION).set(bundle.getString("scanner.path"));
        operation.get(REQUEST_PROPERTIES, PATH, REQUIRED).set(true);
        operation.get(REQUEST_PROPERTIES, PATH, MIN_LENGTH).set(1);
        operation.get(REQUEST_PROPERTIES, RELATIVE_TO, TYPE).set(ModelType.STRING);
        operation.get(REQUEST_PROPERTIES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("scanner.relative-to"));
        operation.get(REQUEST_PROPERTIES, RELATIVE_TO, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, SCAN_ENABLED, TYPE).set(ModelType.BOOLEAN);
        operation.get(REQUEST_PROPERTIES, SCAN_ENABLED, DESCRIPTION).set(bundle.getString("scanner.enabled"));
        operation.get(REQUEST_PROPERTIES, SCAN_ENABLED, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, SCAN_ENABLED, DEFAULT).set(true);
        operation.get(REQUEST_PROPERTIES, SCAN_INTERVAL, TYPE).set(ModelType.INT);
        operation.get(REQUEST_PROPERTIES, SCAN_INTERVAL, DESCRIPTION).set(bundle.getString("scanner.interval"));
        operation.get(REQUEST_PROPERTIES, SCAN_INTERVAL, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, SCAN_INTERVAL, DEFAULT).set(5000);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_ZIPPED, TYPE).set(ModelType.BOOLEAN);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_ZIPPED, DESCRIPTION).set(bundle.getString("scanner.auto.deploy.zipped"));
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_ZIPPED, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_ZIPPED, DEFAULT).set(true);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_EXPLODED, TYPE).set(ModelType.BOOLEAN);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_EXPLODED, DESCRIPTION).set(bundle.getString("scanner.auto.deploy.exploded"));
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_EXPLODED, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, AUTO_DEPLOY_EXPLODED, DEFAULT).set(false);
        operation.get(REQUEST_PROPERTIES, DEPLOYMENT_TIMEOUT, TYPE).set(ModelType.LONG);
        operation.get(REQUEST_PROPERTIES, DEPLOYMENT_TIMEOUT, DESCRIPTION).set(bundle.getString("scanner.deployment.timeout"));
        operation.get(REQUEST_PROPERTIES, DEPLOYMENT_TIMEOUT, REQUIRED).set(false);
        operation.get(REQUEST_PROPERTIES, DEPLOYMENT_TIMEOUT, DEFAULT).set(60L);

        operation.get(REPLY_PROPERTIES).setEmptyObject();

        return operation;
    }

    static final ModelNode getScannerRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode operation = new ModelNode();

        operation.get(OPERATION_NAME).set(REMOVE);
        operation.get(DESCRIPTION).set(bundle.getString("scanner.remove"));
        operation.get(REQUEST_PROPERTIES).setEmptyObject();
        operation.get(REPLY_PROPERTIES).setEmptyObject();

        return operation;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
