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
import static org.jboss.as.logging.CommonAttributes.FILE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.logging.resolvers.SizeResolver;
import org.jboss.as.logging.validators.SizeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class SizeRotatingHandlerResourceDefinition extends AbstractFileHandlerDefinition {

    public static final String SIZE_ROTATING_FILE_HANDLER = "size-rotating-file-handler";
    static final PathElement SIZE_ROTATING_HANDLER_PATH = PathElement.pathElement(SIZE_ROTATING_FILE_HANDLER);

    public static final PropertyAttributeDefinition MAX_BACKUP_INDEX = PropertyAttributeDefinition.Builder.of("max-backup-index", ModelType.INT, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(ElementAttributeMarshaller.VALUE_ATTRIBUTE_MARSHALLER)
            .setDefaultValue(new ModelNode(1))
            .setPropertyName("maxBackupIndex")
            .setValidator(new IntRangeValidator(1, true))
            .build();

    public static final PropertyAttributeDefinition ROTATE_ON_BOOT = PropertyAttributeDefinition.Builder.of("rotate-on-boot", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setPropertyName("rotateOnBoot")
            .build();

    public static final PropertyAttributeDefinition ROTATE_SIZE = PropertyAttributeDefinition.Builder.of("rotate-size", ModelType.STRING)
            .setAllowExpression(true)
            .setAttributeMarshaller(ElementAttributeMarshaller.VALUE_ATTRIBUTE_MARSHALLER)
            .setDefaultValue(new ModelNode("2m"))
            .setPropertyName("rotateSize")
            .setResolver(SizeResolver.INSTANCE)
            .setValidator(new SizeValidator())
            .build();

    static final AttributeDefinition[] ATTRIBUTES = Logging.join(DEFAULT_ATTRIBUTES, AUTOFLUSH, APPEND, MAX_BACKUP_INDEX, ROTATE_SIZE, ROTATE_ON_BOOT, NAMED_FORMATTER, FILE);

    public SizeRotatingHandlerResourceDefinition(final ResolvePathHandler resolvePathHandler, final boolean includeLegacyAttributes) {
        super(SIZE_ROTATING_HANDLER_PATH, SizeRotatingFileHandler.class, resolvePathHandler,
                (includeLegacyAttributes ? Logging.join(ATTRIBUTES, LEGACY_ATTRIBUTES) : ATTRIBUTES));
    }

    @Override
    protected void registerResourceTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder resourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0: {
                resourceBuilder
                        .getAttributeBuilder()
                        .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, AUTOFLUSH, APPEND, FILE, MAX_BACKUP_INDEX, ROTATE_SIZE)
                        .end();
            }
            case VERSION_1_2_0: {
                resourceBuilder
                        .getAttributeBuilder()
                        .setDiscard(new DiscardAttributeValueChecker(new ModelNode(false)), ROTATE_ON_BOOT)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, ROTATE_ON_BOOT)
                        .end();
                if (loggingProfileBuilder != null) {
                    loggingProfileBuilder
                            .getAttributeBuilder()
                            .setDiscard(new DiscardAttributeValueChecker(new ModelNode(false)), ROTATE_ON_BOOT)
                            .addRejectCheck(RejectAttributeChecker.DEFINED, ROTATE_ON_BOOT)
                            .end();
                }
            }
        }

    }

}
