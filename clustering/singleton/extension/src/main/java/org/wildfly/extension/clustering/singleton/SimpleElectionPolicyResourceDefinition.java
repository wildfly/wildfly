/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of a simple election policy resource.
 * @author Paul Ferraro
 */
public class SimpleElectionPolicyResourceDefinition extends ElectionPolicyResourceDefinition {

    static final String PATH_VALUE = "simple";
    static final PathElement PATH = pathElement(PATH_VALUE);

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        POSITION("position", ModelType.INT, new ModelNode(0)),
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .build();
        }

        @Override
        public SimpleAttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SimpleElectionPolicyResourceDefinition() {
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), SimpleElectionPolicyServiceConfigurator::new);
    }
}
