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

import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENABLED;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.logmanager.handlers.FileHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class FileHandlerResourceDefinition extends AbstractFileHandlerDefinition {

    public static final String FILE_HANDLER = "file-handler";
    static final PathElement FILE_HANDLER_PATH = PathElement.pathElement(FILE_HANDLER);

    static final AttributeDefinition[] ATTRIBUTES = Logging.join(DEFAULT_ATTRIBUTES, AUTOFLUSH, APPEND, FILE);

    public FileHandlerResourceDefinition(final ResolvePathHandler resolvePathHandler, final boolean includeLegacyAttributes) {
        super(FILE_HANDLER_PATH, FileHandler.class, resolvePathHandler, (
                includeLegacyAttributes ? Logging.join(ATTRIBUTES, LEGACY_ATTRIBUTES) : ATTRIBUTES));
    }

    /**
     * Register the transformers for the console handler.
     *
     * @param registration      the root resource
     * @param loggingProfileReg the logging profile resource which will be discarded
     */
    static void registerTransformers(final TransformersSubRegistration registration, final TransformersSubRegistration loggingProfileReg) {
        registerTransformers(registration, loggingProfileReg, FILE_HANDLER_PATH, AUTOFLUSH, APPEND, FILE);
    }
}
