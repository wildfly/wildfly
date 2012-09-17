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

package org.jboss.as.controller.client;

import static org.jboss.as.controller.client.ControllerClientLogger.ROOT_LOGGER;
import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * An abstraction of user defined metadata that can be given
 * to the {@link DeploymentPlanBuilder}.
 *
 * This object will end up as an attachment to the {@link DeploymentUnit}
 * under key {@link Attachments#DEPLOYMENT_METADATA}
 *
 * Current supported types are
 *
 * {@link ModelType#BIG_DECIMAL}
 * {@link ModelType#BIG_INTEGER}
 * {@link ModelType#BOOLEAN}
 * {@link ModelType#DOUBLE}
 * {@link ModelType#INT}
 * {@link ModelType#LONG}
 * {@link ModelType#STRING}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Mar-2012
 */
public class DeploymentMetadata {

    private final Map<String, Object> userdata = new HashMap<String, Object>();
    private final ModelNode metadata;

    public static DeploymentMetadata UNDEFINED = new DeploymentMetadata(new ModelNode());

    public DeploymentMetadata(Map<String, Object> usermap) {
        metadata = new ModelNode();
        if (usermap != null) {
            userdata.putAll(usermap);
            for (Entry<String, Object> entry : usermap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof BigDecimal) {
                    metadata.get(key).set((BigDecimal) value);
                } else if (value instanceof BigInteger) {
                    metadata.get(key).set((BigInteger) value);
                } else if (value instanceof Boolean) {
                    metadata.get(key).set((Boolean) value);
                } else if (value instanceof Double) {
                    metadata.get(key).set((Double) value);
                } else if (value instanceof Integer) {
                    metadata.get(key).set((Integer) value);
                } else if (value instanceof Long) {
                    metadata.get(key).set((Long) value);
                } else if (value instanceof String) {
                    metadata.get(key).set((String) value);
                } else {
                    ROOT_LOGGER.invalidMetadataType(key);
                }
            }
        }
    }

    public DeploymentMetadata(ModelNode metadata) {
        if (metadata == null)
            throw MESSAGES.nullVar("metadata");
        this.metadata = metadata;
        if (metadata.isDefined()) {
            for (Property entry : metadata.asPropertyList()) {
                String key = entry.getName();
                ModelNode value = entry.getValue();
                ModelType type = value.getType();
                switch (type) {
                    case BIG_DECIMAL:
                        userdata.put(key, value.asBigDecimal());
                        break;
                    case BIG_INTEGER:
                        userdata.put(key, value.asBigInteger());
                        break;
                    case BOOLEAN:
                        userdata.put(key, value.asBoolean());
                        break;
                    case DOUBLE:
                        userdata.put(key, value.asDouble());
                        break;
                    case INT:
                        userdata.put(key, value.asInt());
                        break;
                    case LONG:
                        userdata.put(key, value.asLong());
                        break;
                    case STRING:
                        userdata.put(key, value.asString());
                        break;
                    default:
                        ROOT_LOGGER.invalidMetadataType(key);
                }
            }
        }
    }

    /**
     * Get the the metadata value for the given key.
     * @return The value or <code>null</code>
     */
    public Object getValue(String key) {
        return userdata != null ? userdata.get(key) : null;
    }

    /**
     * Get the user defined metadata map.
     * @return The metadata map or an empty map.
     */
    public Map<String, Object> getUserdata() {
        return Collections.unmodifiableMap(userdata);
    }

    /**
     * Get the model node that corresponds to the user defined metadata.
     * @return The model node, which may be 'undefined'
     */
    public ModelNode getModelNode() {
        return metadata;
    }

    /**
     * Return true if undefined
     */
    public boolean isDefined() {
        return metadata.isDefined();
    }

    @Override
    public String toString() {
        return metadata.toString();
    }
}
