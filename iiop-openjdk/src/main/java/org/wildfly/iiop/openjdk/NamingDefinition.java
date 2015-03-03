/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class NamingDefinition extends PersistentResourceDefinition {

    protected static final AttributeDefinition ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(Constants.ROOT_CONTEXT_INIT_REF))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    protected static final AttributeDefinition EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            Constants.NAMING_EXPORT_CORBALOC, ModelType.STRING, true)
            .setDefaultValue(AttributeConstants.TRUE_PROPERTY).setValidator(AttributeConstants.TRUE_FALSE_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    private static final List<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
            ROOT_CONTEXT, EXPORT_CORBALOC));

    private NamingDefinition() {
        super(IIOPExtension.PATH_NAMING, IIOPExtension.getResourceDescriptionResolver(Constants.NAMING),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    static final NamingDefinition INSTANCE = new NamingDefinition();

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
