/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;


/**
 * @author Emanuel Muckenhuber
 */
class JaxrsSubsystemProviders {

    static final String RESOURCE_NAME = JaxrsSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystemDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("jaxrs"));
        return subsystem;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("jaxrs.add"));
        return subsystem;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
