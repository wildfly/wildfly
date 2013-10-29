/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.wildfly.extension.io.OptionList;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class RemotingEndpointResource extends PersistentResourceDefinition {
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(CommonAttributes.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();


    static final List<OptionAttributeDefinition> OPTIONS = OptionList.builder()
            .addOption(RemotingOptions.SEND_BUFFER_SIZE, "send-buffer-size", new ModelNode(RemotingOptions.DEFAULT_SEND_BUFFER_SIZE))
            .addOption(RemotingOptions.RECEIVE_BUFFER_SIZE, "receive-buffer-size", new ModelNode(RemotingOptions.DEFAULT_RECEIVE_BUFFER_SIZE))
            .addOption(RemotingOptions.BUFFER_REGION_SIZE, "buffer-region-size")
            .addOption(RemotingOptions.TRANSMIT_WINDOW_SIZE, "transmit-window-size", new ModelNode(RemotingOptions.OUTGOING_CHANNEL_DEFAULT_RECEIVE_WINDOW_SIZE))
            .addOption(RemotingOptions.RECEIVE_WINDOW_SIZE, "receive-window-size", new ModelNode(RemotingOptions.INCOMING_CHANNEL_DEFAULT_TRANSMIT_WINDOW_SIZE))
            .addOption(RemotingOptions.MAX_OUTBOUND_CHANNELS, "max-outbound-channels", new ModelNode(RemotingOptions.DEFAULT_MAX_OUTBOUND_CHANNELS))
            .addOption(RemotingOptions.MAX_INBOUND_CHANNELS, "max-inbound-channels", new ModelNode(RemotingOptions.DEFAULT_MAX_INBOUND_CHANNELS))
            .addOption(RemotingOptions.AUTHORIZE_ID, "authorize-id")
            .addOption(RemotingOptions.AUTH_REALM, "auth-realm")
            .addOption(RemotingOptions.AUTHENTICATION_RETRIES, "authentication-retries", new ModelNode(RemotingOptions.DEFAULT_AUTHENTICATION_RETRIES))
            .addOption(RemotingOptions.MAX_OUTBOUND_MESSAGES, "max-outbound-messages", new ModelNode(RemotingOptions.OUTGOING_CHANNEL_DEFAULT_MAX_OUTBOUND_MESSAGES))
            .addOption(RemotingOptions.MAX_INBOUND_MESSAGES, "max-inbound-messages", new ModelNode(RemotingOptions.INCOMING_CHANNEL_DEFAULT_MAX_OUTBOUND_MESSAGES))
            .addOption(RemotingOptions.HEARTBEAT_INTERVAL, "heartbeat-interval", new ModelNode(RemotingOptions.DEFAULT_HEARTBEAT_INTERVAL))
            .addOption(RemotingOptions.MAX_INBOUND_MESSAGE_SIZE, "max-inbound-message-size", new ModelNode(RemotingOptions.DEFAULT_MAX_INBOUND_MESSAGE_SIZE))
            .addOption(RemotingOptions.MAX_OUTBOUND_MESSAGE_SIZE, "max-outbound-message-size", new ModelNode(RemotingOptions.DEFAULT_MAX_OUTBOUND_MESSAGE_SIZE))
            .addOption(RemotingOptions.SERVER_NAME, "server-name")
            .addOption(RemotingOptions.SASL_PROTOCOL, "sasl-protocol", new ModelNode(RemotingOptions.DEFAULT_SASL_PROTOCOL))
            .build();

    protected static final PathElement ENDPOINT_PATH = PathElement.pathElement("configuration", "endpoint");
    protected static final Collection ATTRIBUTES;

    static final RemotingEndpointResource INSTANCE = new RemotingEndpointResource();

    static {
        ATTRIBUTES = new LinkedHashSet<AttributeDefinition>(Arrays.asList(WORKER));
        ATTRIBUTES.addAll(OPTIONS);
    }

    private RemotingEndpointResource() {
        super(ENDPOINT_PATH, RemotingExtension.getResourceDescriptionResolver("endpoint"));
    }

    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return ATTRIBUTES;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        super.registerAddOperation(resourceRegistration, new RemotingEndpointAdd(), OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(resourceRegistration, ReloadRequiredRemoveStepHandler.INSTANCE, OperationEntry.Flag.RESTART_NONE);
    }


}
