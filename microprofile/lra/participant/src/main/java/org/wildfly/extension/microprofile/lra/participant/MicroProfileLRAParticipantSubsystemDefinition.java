/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import java.util.Arrays;
import java.util.Collection;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME;

public class MicroProfileLRAParticipantSubsystemDefinition extends PersistentResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileLRAParticipantExtension.class);

    private static final String LRA_PARTICIPANT_CAPABILITY_NAME = "org.wildfly.microprofile.lra.participant";

    public static final String COORDINATOR_URL_PROP = "lra.coordinator.url";

    static final RuntimeCapability<Void> LRA_PARTICIPANT_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_PARTICIPANT_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();

    static final SimpleAttributeDefinition LRA_COORDINATOR_URL =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.LRA_COORDINATOR_URL, ModelType.STRING, true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(CommonAttributes.DEFAULT_COORDINATOR_URL))
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.LRA_COORDINATOR_URL)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition PROXY_SERVER =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.PROXY_SERVER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.PROXY_SERVER)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_SERVER))
            .build();

    static final SimpleAttributeDefinition PROXY_HOST =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.PROXY_HOST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.PROXY_HOST)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_HOST))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {LRA_COORDINATOR_URL, PROXY_SERVER, PROXY_HOST};

    MicroProfileLRAParticipantSubsystemDefinition() {
        super(new Parameters(PATH, RESOLVER)
            .setAddHandler(new MicroProfileLRAParticipantAdd())
            .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
            .setCapabilities(LRA_PARTICIPANT_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}