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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.HandlerOperations.HandlerUpdateOperationStepHandler;
import static org.jboss.as.logging.HandlerOperations.LogHandlerWriteAttributeHandler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
abstract class AbstractHandlerDefinition extends SimpleResourceDefinition {

    public static final String UPDATE_OPERATION_NAME = "update-properties";
    public static final String CHANGE_LEVEL_OPERATION_NAME = "change-log-level";

    static final AttributeDefinition[] DEFAULT_WRITABLE_ATTRIBUTES = {
            LEVEL,
            ENCODING,
            FORMATTER,
            FILTER,
    };

    static final AttributeDefinition[] DEFAULT_READ_ONLY_ATTRIBUTES = {
            NAME
    };

    private final LogHandlerWriteAttributeHandler writeHandler;
    private final AttributeDefinition[] writableAttributes;
    private final AttributeDefinition[] readOnlyAttributes;

    protected AbstractHandlerDefinition(final PathElement path, final String key,
                                        final LoggingOperations.LoggingAddOperationStepHandler addHandler,
                                        final AttributeDefinition... writableAttributes) {
        this(path, key, addHandler, DEFAULT_READ_ONLY_ATTRIBUTES, writableAttributes);
    }

    protected AbstractHandlerDefinition(final PathElement path, final String key,
                                        final LoggingOperations.LoggingAddOperationStepHandler addHandler,
                                        final AttributeDefinition[] readOnlyAttributes,
                                        final AttributeDefinition... writableAttributes) {
        super(path,
                LoggingExtension.getResourceDescriptionResolver("handler"),
                addHandler,
                HandlerOperations.REMOVE_HANDLER);
        this.writableAttributes = writableAttributes;
        writeHandler = new LogHandlerWriteAttributeHandler(this.writableAttributes);
        this.readOnlyAttributes = readOnlyAttributes;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : writableAttributes) {
            resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
        }
        for (AttributeDefinition def : readOnlyAttributes) {
            resourceRegistration.registerReadOnlyAttribute(def, null);
        }
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        final ResourceDescriptionResolver resolver = getResourceDescriptionResolver();
        registration.registerOperationHandler(new SimpleOperationDefinition(ENABLE, resolver), HandlerOperations.ENABLE_HANDLER);
        registration.registerOperationHandler(new SimpleOperationDefinition(DISABLE, resolver), HandlerOperations.DISABLE_HANDLER);
        registration.registerOperationHandler(new SimpleOperationDefinition(CHANGE_LEVEL_OPERATION_NAME, resolver, CommonAttributes.LEVEL), HandlerOperations.CHANGE_LEVEL);
        registration.registerOperationHandler(new SimpleOperationDefinition(UPDATE_OPERATION_NAME, resolver, writableAttributes), new HandlerUpdateOperationStepHandler(writableAttributes));
    }

    /**
     * Appends the default writable attributes with the attributes provided.
     * <p/>
     * Uniqueness is guaranteed on the returned array.
     *
     * @param attributes the attributes to add
     *
     * @return an array of the attributes
     */
    static AttributeDefinition[] appendDefaultWritableAttributes(final AttributeDefinition... attributes) {
        return joinUnique(DEFAULT_WRITABLE_ATTRIBUTES, attributes);
    }

    /**
     * Appends the default writable attributes with the attributes provided.
     * <p/>
     * Uniqueness is guaranteed on the returned array.
     *
     * @param attributes the attributes to add
     *
     * @return an array of the attributes
     */
    static AttributeDefinition[] appendDefaultReadOnlyAttributes(final AttributeDefinition... attributes) {
        return joinUnique(DEFAULT_READ_ONLY_ATTRIBUTES, attributes);
    }

    /**
     * Joins the two arrays and guarantees a unique array.
     * <p/>
     * The array returned may contain fewer attributes than the two arrays combined. Any duplicate attributes are
     * ignored.
     *
     * @param base       the base attributes
     * @param attributes the attributes to add
     *
     * @return an array of the attributes
     */
    static AttributeDefinition[] joinUnique(final AttributeDefinition[] base, final AttributeDefinition... attributes) {
        final Map<String, AttributeDefinition> result = new LinkedHashMap<String, AttributeDefinition>();
        if (base != null) {
            for (AttributeDefinition attr : base) {
                result.put(attr.getName(), attr);
            }
        }
        if (attributes != null) {
            for (AttributeDefinition attr : attributes) {
                if (!result.containsKey(attr.getName()))
                    result.put(attr.getName(), attr);
            }
        }
        return result.values().toArray(new AttributeDefinition[result.size()]);
    }
}
