/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>  2014 Red Hat Inc.
 * @author Paul Ferraro
 */
class SingleSignOnDefinition extends PersistentResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DOMAIN(Constants.DOMAIN, ModelType.STRING, null),
        PATH("path", ModelType.STRING, new ModelNode("/")),
        HTTP_ONLY("http-only", ModelType.BOOLEAN, new ModelNode(false)),
        SECURE("secure", ModelType.BOOLEAN, new ModelNode(false)),
        COOKIE_NAME("cookie-name", ModelType.STRING, new ModelNode("JSESSIONIDSSO")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SingleSignOnDefinition() {
        super(new Parameters(UndertowExtension.PATH_SSO, UndertowExtension.getResolver(Constants.SINGLE_SIGN_ON)));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        // Attributes will be registered by the parent implementation
        return Collections.emptyList();
    }
}
