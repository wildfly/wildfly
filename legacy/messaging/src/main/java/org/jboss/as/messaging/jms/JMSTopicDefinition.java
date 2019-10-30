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

package org.jboss.as.messaging.jms;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;


/**
 * JMS Topic resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class JMSTopicDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JMS_TOPIC);

    public static final AttributeDefinition[] ATTRIBUTES = { CommonAttributes.DESTINATION_ENTRIES };

    public static final JMSTopicDefinition INSTANCE = new JMSTopicDefinition();
    private final List<AccessConstraintDefinition> accessConstraints;

    public JMSTopicDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_TOPIC),
                ATTRIBUTES);
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MessagingExtension.SUBSYSTEM_NAME, CommonAttributes.JMS_TOPIC);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
