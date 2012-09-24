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
 */package org.jboss.as.domain.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.dmr.ModelNode;

/**
 * Model description for the domain root.
 *
 * @author Brian Stansberry
 */
@Deprecated
public class DomainRootDescription {

    private static final String RESOURCE_NAME = DomainRootDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return getResourceDescriptionResolver(keyPrefix, true);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, final boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, DomainRootDescription.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public static ModelNode getRestartServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.RESTART_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("domain.servers.restart"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getStopServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.STOP_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("domain.servers.stop"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    public static ModelNode getStartServersOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(DomainServerLifecycleHandlers.START_SERVERS_NAME);
        root.get(DESCRIPTION).set(bundle.getString("domain.servers.start"));
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
