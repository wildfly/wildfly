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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.LONG;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.dmr.ModelType;


/**
 * Queue resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class QueueDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.QUEUE);

    public static final SimpleAttributeDefinition ADDRESS = create("queue-address", ModelType.STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = { ADDRESS, CommonAttributes.FILTER, CommonAttributes.DURABLE };

    public static final SimpleAttributeDefinition EXPIRY_ADDRESS = create(CommonAttributes.EXPIRY_ADDRESS)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition DEAD_LETTER_ADDRESS = create(CommonAttributes.DEAD_LETTER_ADDRESS)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition ID= create("id", LONG)
            .setStorageRuntime()
            .build();

    private final List<AccessConstraintDefinition> accessConstraints;

    static final QueueDefinition INSTANCE = new QueueDefinition();

    private QueueDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.QUEUE),
                ATTRIBUTES);
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MessagingExtension.SUBSYSTEM_NAME, CommonAttributes.QUEUE);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
