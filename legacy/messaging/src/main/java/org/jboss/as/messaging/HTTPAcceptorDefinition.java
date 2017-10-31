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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.messaging.CommonAttributes.HTTP_ACCEPTOR;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * HTTP acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class HTTPAcceptorDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(HTTP_ACCEPTOR);

    static final SimpleAttributeDefinition HTTP_LISTENER = create(CommonAttributes.HTTP_LISTENER, ModelType.STRING)
            .setRequired(true)
            .build();

    static AttributeDefinition[] ATTRIBUTES = { HTTP_LISTENER };

    static final HTTPAcceptorDefinition INSTANCE = new HTTPAcceptorDefinition();

    private HTTPAcceptorDefinition() {
        super(PATH,
                new StandardResourceDescriptionResolver(CommonAttributes.ACCEPTOR, MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(HTTP_ACCEPTOR);
                    }
                },
                ATTRIBUTES);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        registry.registerSubModel(new TransportParamDefinition());
    }
}
