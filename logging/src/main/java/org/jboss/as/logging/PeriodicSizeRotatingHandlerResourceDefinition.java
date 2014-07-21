/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.PeriodicHandlerResourceDefinition.SUFFIX;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.ROTATE_ON_BOOT;
import static org.jboss.as.logging.SizeRotatingHandlerResourceDefinition.ROTATE_SIZE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;

/**
 * Resource for a {@link org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class PeriodicSizeRotatingHandlerResourceDefinition extends AbstractFileHandlerDefinition {

    public static final String PERIODIC_SIZE_ROTATING_FILE_HANDLER = "periodic-size-rotating-file-handler";
    static final PathElement PERIODIC_SIZE_ROTATING_HANDLER_PATH = PathElement.pathElement(PERIODIC_SIZE_ROTATING_FILE_HANDLER);

    static final AttributeDefinition[] ATTRIBUTES = Logging.join(DEFAULT_ATTRIBUTES, AUTOFLUSH, APPEND, MAX_BACKUP_INDEX, ROTATE_SIZE, ROTATE_ON_BOOT, SUFFIX, NAMED_FORMATTER, FILE);

    public PeriodicSizeRotatingHandlerResourceDefinition(final ResolvePathHandler resolvePathHandler) {
        super(PERIODIC_SIZE_ROTATING_HANDLER_PATH, false, PeriodicSizeRotatingFileHandler.class, resolvePathHandler, ATTRIBUTES);
    }

    @Override
    protected void registerResourceTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder resourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0:
            case VERSION_1_2_0:
            case VERSION_1_3_0:
            case VERSION_1_4_0:{
                resourceBuilder.rejectChildResource(PERIODIC_SIZE_ROTATING_HANDLER_PATH);
                if (loggingProfileBuilder != null) {
                    loggingProfileBuilder.rejectChildResource(PERIODIC_SIZE_ROTATING_HANDLER_PATH);
                }
            }
        }

    }

}
