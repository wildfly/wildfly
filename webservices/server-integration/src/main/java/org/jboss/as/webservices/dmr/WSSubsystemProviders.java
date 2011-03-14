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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.webservices.dmr.Constants.CONFIGURATION;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_SOAP_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WEBSERVICE_HOST;
import static org.jboss.as.webservices.dmr.Constants.WEBSERVICE_PORT;
import static org.jboss.as.webservices.dmr.Constants.WEBSERVICE_SECURE_PORT;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class WSSubsystemProviders {
    static final String RESOURCE_NAME = WSSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static class Descriptions {
        static ModelNode getSubsystem(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();

            subsystem.get(DESCRIPTION).set(bundle.getString("ws"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.WEBSERVICES_1_0.getUriString());

            subsystem.get(ATTRIBUTES, CONFIGURATION, DESCRIPTION).set(bundle.getString("configuration"));
            subsystem.get(ATTRIBUTES, CONFIGURATION, TYPE).set(ModelType.OBJECT);
            subsystem.get(ATTRIBUTES, CONFIGURATION, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, DESCRIPTION).set(bundle.getString("modify-soap-address"));
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, DESCRIPTION).set(bundle.getString("web-service-host"));
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, DESCRIPTION).set(bundle.getString("web-service-port"));
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, DESCRIPTION).set(bundle.getString("web-service-secure-port"));
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, REQUIRED).set(false);

            return subsystem;
        }

        static ModelNode getSubsystemAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("ws.add"));

            op.get(REQUEST_PROPERTIES, CONFIGURATION, DESCRIPTION).set(bundle.getString("configuration"));
            op.get(REQUEST_PROPERTIES, CONFIGURATION, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, DESCRIPTION).set(bundle.getString("modify-soap-address"));
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, TYPE).set(ModelType.BOOLEAN);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, MODIFY_SOAP_ADDRESS, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, DESCRIPTION).set(bundle.getString("web-service-host"));
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_HOST, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, DESCRIPTION).set(bundle.getString("web-service-port"));
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, TYPE).set(ModelType.INT);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_PORT, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, DESCRIPTION).set(bundle.getString("web-service-secure-port"));
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, TYPE).set(ModelType.INT);
            op.get(REQUEST_PROPERTIES, CONFIGURATION, VALUE_TYPE, WEBSERVICE_SECURE_PORT, REQUIRED).set(false);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }
    }

}
