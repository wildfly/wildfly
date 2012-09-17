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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;

/**
 * Model descriptions for deployment resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Deprecated
public class DeploymentDescription {

    private static final String RESOURCE_NAME = DeploymentDescription.class.getPackage().getName() + ".LocalDescriptions";

    private DeploymentDescription() {
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

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(String[] args) {
    }
}
