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
 */package org.jboss.as.host.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for the host model root.
 *
 * @author Brian Stansberry
 */
@Deprecated
public class HostRootDescription {

    private static final String BLOCKING = "blocking";

    private static final String RESOURCE_NAME = HostEnvironmentResourceDescription.class.getPackage().getName() + ".LocalDescriptions";

    @Deprecated
    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return getResourceDescriptionResolver(keyPrefix, true);
    }
    @Deprecated
    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, final boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, HostEnvironmentResourceDescription.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public static ModelNode getStartServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("start-server");
        root.get(DESCRIPTION).set(bundle.getString("host.start-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.start-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, BLOCKING, DEFAULT).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, DESCRIPTION).set(bundle.getString("host.start-server.blocking"));
        root.get(REQUEST_PROPERTIES, BLOCKING, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.start-server.reply"));
        return root;
    }

    public static ModelNode getRestartServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("restart-server");
        root.get(DESCRIPTION).set(bundle.getString("host.restart-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.restart-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, BLOCKING, DEFAULT).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, DESCRIPTION).set(bundle.getString("host.restart-server.blocking"));
        root.get(REQUEST_PROPERTIES, BLOCKING, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.restart-server.reply"));
        return root;
    }

    public static ModelNode getStopServerOperation(final Locale locale) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set("stop-server");
        root.get(DESCRIPTION).set(bundle.getString("host.stop-server"));
        root.get(REQUEST_PROPERTIES, SERVER, TYPE).set(ModelType.STRING);
        root.get(REQUEST_PROPERTIES, SERVER, DESCRIPTION).set(bundle.getString("host.stop-server.server"));
        root.get(REQUEST_PROPERTIES, SERVER, REQUIRED).set(true);
        root.get(REQUEST_PROPERTIES, SERVER, MIN_LENGTH).set(1);
        root.get(REQUEST_PROPERTIES, SERVER, NILLABLE).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, BLOCKING, DEFAULT).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, DESCRIPTION).set(bundle.getString("host.stop-server.blocking"));
        root.get(REQUEST_PROPERTIES, BLOCKING, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, BLOCKING, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        root.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("host.stop-server.reply"));
        return root;
    }

    public static ModelNode getHostShutdownHandler(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(HostShutdownHandler.OPERATION_NAME);
        root.get(DESCRIPTION).set(bundle.getString("host.shutdown"));
        root.get(REQUEST_PROPERTIES, RESTART, TYPE).set(ModelType.BOOLEAN);
        root.get(REQUEST_PROPERTIES, RESTART, DESCRIPTION).set(bundle.getString("host.shutdown.restart"));
        root.get(REQUEST_PROPERTIES, RESTART, DEFAULT).set(false);
        root.get(REQUEST_PROPERTIES, RESTART, REQUIRED).set(false);
        root.get(REQUEST_PROPERTIES, RESTART, NILLABLE).set(true);
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getLocalDomainControllerAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(LocalDomainControllerAddHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.local.add"));

        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }


    public static ModelNode getLocalDomainControllerRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(LocalDomainControllerRemoveHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.local.remove"));

        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    public static ModelNode getRemoteDomainControllerAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(RemoteDomainControllerAddHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.remote.add"));

        result.get(REQUEST_PROPERTIES, HOST, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES, HOST, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.host"));
        result.get(REQUEST_PROPERTIES, HOST, REQUIRED).set(true);
        result.get(REQUEST_PROPERTIES, HOST, EXPRESSIONS_ALLOWED).set(true);
        result.get(REQUEST_PROPERTIES, HOST, MIN_LENGTH).set(1);
        result.get(REQUEST_PROPERTIES, PORT, TYPE).set(ModelType.INT);
        result.get(REQUEST_PROPERTIES, PORT, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.port"));
        result.get(REQUEST_PROPERTIES, PORT, REQUIRED).set(true);
        result.get(REQUEST_PROPERTIES, PORT, EXPRESSIONS_ALLOWED).set(true);
        result.get(REQUEST_PROPERTIES, PORT, MIN).set(1);
        result.get(REQUEST_PROPERTIES, PORT, MAX).set(65535);

        result.get(REQUEST_PROPERTIES, USERNAME, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES, USERNAME, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.username"));
        result.get(REQUEST_PROPERTIES, USERNAME, REQUIRED).set(false);
        result.get(REQUEST_PROPERTIES, USERNAME, EXPRESSIONS_ALLOWED).set(true);
        result.get(REQUEST_PROPERTIES, USERNAME, MIN_LENGTH).set(1);

        result.get(REQUEST_PROPERTIES, SECURITY_REALM, TYPE).set(ModelType.STRING);
        result.get(REQUEST_PROPERTIES, SECURITY_REALM, DESCRIPTION).set(bundle.getString("host.domain-controller.remote.security-realm"));
        result.get(REQUEST_PROPERTIES, SECURITY_REALM, REQUIRED).set(false);
        result.get(REQUEST_PROPERTIES, SECURITY_REALM, EXPRESSIONS_ALLOWED).set(false);
        result.get(REQUEST_PROPERTIES, SECURITY_REALM, MIN_LENGTH).set(1);

        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    public static ModelNode getRemoteDomainControllerRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(RemoteDomainControllerRemoveHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("host.domain-controller.remote.remove"));

        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
