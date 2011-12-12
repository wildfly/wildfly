/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler responsible for adding JAXR attributes to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Nov-2011
 */
public class JAXRWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    static AttributeDefinition CONNECTION_FACTORY_ATTRIBUTE = new JAXRAttributeDefinition(ModelConstants.CONNECTION_FACTORY);
    static AttributeDefinition PUBLISH_URL_ATTRIBUTE = new JAXRAttributeDefinition(ModelConstants.PUBLISH_URL);
    static AttributeDefinition QUERY_URL_ATTRIBUTE = new JAXRAttributeDefinition(ModelConstants.QUERY_URL);

    private final JAXRConfiguration config;

    JAXRWriteAttributeHandler(JAXRConfiguration config) {
        this.config = config;
    }

    void registerAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(CONNECTION_FACTORY_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(PUBLISH_URL_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(QUERY_URL_ATTRIBUTE, null, this);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        applyUpdateToConfig(config, attributeName, resolvedValue);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        applyUpdateToConfig(config, attributeName, valueToRestore);
    }

    public static void applyUpdateToConfig(JAXRConfiguration config, String attributeName, ModelNode attributeValue) {
        if (attributeValue.isDefined()) {
            config.applyUpdateToConfig(attributeName, attributeValue.asString());
        }
    }

    private static class JAXRAttributeDefinition extends SimpleAttributeDefinition {
        JAXRAttributeDefinition(String name) {
            super(name, ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);
        }
    }
}
