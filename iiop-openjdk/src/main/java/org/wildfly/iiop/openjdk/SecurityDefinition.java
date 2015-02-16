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
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class SecurityDefinition extends PersistentResourceDefinition {

    public static final AttributeDefinition SUPPORT_SSL = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SUPPORT_SSL, ModelType.STRING, true)
            .setDefaultValue(AttributeConstants.FALSE_PROPERTY)
            .setValidator(AttributeConstants.TRUE_FALSE_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SECURITY_DOMAIN, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition ADD_COMPONENT_INTERCEPTOR = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, ModelType.STRING, true)
            .setDefaultValue(AttributeConstants.TRUE_PROPERTY)
            .setValidator(AttributeConstants.TRUE_FALSE_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition CLIENT_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_CLIENT_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(AttributeConstants.SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition CLIENT_REQUIRES = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_CLIENT_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(AttributeConstants.SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition SERVER_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SERVER_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(AttributeConstants.SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition SERVER_REQUIRES = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SERVER_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(AttributeConstants.SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(AttributeConstants.IIOP_SECURITY_DEF)
            .build();

    private static final List<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(SUPPORT_SSL,
            SECURITY_DOMAIN, ADD_COMPONENT_INTERCEPTOR, CLIENT_SUPPORTS, CLIENT_REQUIRES, SERVER_SUPPORTS, SERVER_REQUIRES));

    static final SecurityDefinition INSTANCE = new SecurityDefinition();


    private SecurityDefinition() {
        super(IIOPExtension.PATH_SECURITY, IIOPExtension.getResourceDescriptionResolver(Constants.SECURITY),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }


}
