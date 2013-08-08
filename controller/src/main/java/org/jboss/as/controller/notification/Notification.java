/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.notification;

import org.jboss.dmr.ModelNode;

/**
 * A notification emitted by a resource and handled by {@link NotificationHandler}.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class Notification {

    public static final String RESOURCE = "resource";
    public static final String TYPE = "type";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String DATA = "data";

    private final String type;
    private final ModelNode resource;
    private final String message;
    private final long timestamp;
    private final ModelNode data;

    public Notification(String type, ModelNode resource, String message) {
        this(type, resource, message, null);
    }

    public Notification(String type, ModelNode resource, String message, ModelNode data) {
        this(type, resource, message, System.currentTimeMillis(), data);
    }

    private Notification(String type, ModelNode resource, String message, long timestamp, ModelNode data) {
        this.type = type;
        this.resource = resource;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public ModelNode getResource() {
        return resource;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ModelNode getData() {
        return data;
    }

    public ModelNode toModelNode() {
        ModelNode node = new ModelNode();
        node.get(TYPE).set(type);
        node.get(RESOURCE).set(resource);
        node.get(TIMESTAMP).set(timestamp);
        node.get(MESSAGE).set(message);
        if (data != null) {
            node.get(DATA).set(data);
        }
        node.protect();
        return node;
    }

    public static Notification fromModelNode(ModelNode node) {
        String type = node.require(TYPE).asString();
        ModelNode resource = node.require(RESOURCE);
        long timestamp = node.require(TIMESTAMP).asLong();
        String message = node.require(MESSAGE).asString();
        ModelNode data = node.hasDefined(DATA)? node.get(DATA): null;
        return new Notification(type, resource, message, timestamp, data);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", resource=" + resource +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}
