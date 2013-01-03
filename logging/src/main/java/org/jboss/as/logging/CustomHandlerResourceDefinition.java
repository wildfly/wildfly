/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.MODULE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class CustomHandlerResourceDefinition extends AbstractHandlerDefinition {
    static final PathElement CUSTOM_HANDLE_PATH = PathElement.pathElement(CommonAttributes.CUSTOM_HANDLER);

    /*
    * Configurations
    */
    static final AttributeDefinition[] READ_ONLY_ATTRIBUTES = {CLASS, MODULE};
    static final AttributeDefinition[] WRITABLE_ATTRIBUTES = Logging.join(DEFAULT_ATTRIBUTES, CommonAttributes.PROPERTIES);
    // Add attributes are a combination of writable and read-only attributes
    static final AttributeDefinition[] ADD_ATTRIBUTES = Logging.join(WRITABLE_ATTRIBUTES, READ_ONLY_ATTRIBUTES);

    public CustomHandlerResourceDefinition(final boolean includeLegacyAttributes) {
        super(CUSTOM_HANDLE_PATH, null,
                (includeLegacyAttributes ? Logging.join(ADD_ATTRIBUTES, LEGACY_ATTRIBUTES) : ADD_ATTRIBUTES),
                READ_ONLY_ATTRIBUTES,
                (includeLegacyAttributes ? Logging.join(WRITABLE_ATTRIBUTES, LEGACY_ATTRIBUTES) : WRITABLE_ATTRIBUTES));
    }
}
