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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.as.jdkorb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * <p>
 * This class contains all JdkORB subsystem attribute definitions.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class JdkORBSubsystemDefinitions {

    private static final ModelNode DEFAULT_DISABLED_PROPERTY = new ModelNode().set("off");

    private static final ModelNode DEFAULT_ENABLED_PROPERTY = new ModelNode().set("on");

    private static final ParameterValidator SSL_CONFIG_VALIDATOR =
            new EnumValidator<SSLConfigValue>(SSLConfigValue.class, true, false);

    private static final ParameterValidator ON_OFF_VALIDATOR = new EnumValidator<TransactionsAllowedValues>(
            TransactionsAllowedValues.class, true, false, TransactionsAllowedValues.ON, TransactionsAllowedValues.OFF);

    static final SensitivityClassification JDKORB_SECURITY =
            new SensitivityClassification(JdkORBExtension.SUBSYSTEM_NAME, "jdkorb-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition JDKORB_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(JDKORB_SECURITY);

    public static final SimpleAttributeDefinition ORB_PERSISTENT_SERVER_ID = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_PERSISTENT_SERVER_ID, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_GIOP_VERSION = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_GIOP_VERSION, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1.2"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition ORB_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_SOCKET_BINDING, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jdkorb"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition ORB_SSL_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_SSL_SOCKET_BINDING, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jdkorb-ssl"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    // connection attribute definitions.

    public static final SimpleAttributeDefinition ORB_TCP_HIGH_WATER_MARK = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.TCP_HIGH_WATER_MARK, ModelType.INT, true)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition ORB_TCP_NUMBER_TO_RECLAIM_PROPERTY = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.TCP_NUMBER_TO_RECLAIM, ModelType.INT, true)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    // initializers attribute definitions.
    public static final SimpleAttributeDefinition ORB_INIT_SECURITY = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_INIT_SECURITY, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(new EnumValidator<SecurityAllowedValues>(SecurityAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition ORB_INIT_TX = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_INIT_TRANSACTIONS, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(new EnumValidator<TransactionsAllowedValues>(TransactionsAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    // naming attribute definitions.
    public static final SimpleAttributeDefinition NAMING_ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(JdkORBSubsystemConstants.JDKORB_NAME_SERVICE_INIT_REF))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition NAMING_EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.NAMING_EXPORT_CORBALOC, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    // security attribute definitions.
    public static final SimpleAttributeDefinition SECURITY_SUPPORT_SSL = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_SUPPORT_SSL, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_SECURITY_DOMAIN, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_ADD_COMPONENT_INTERCEPTOR = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_CLIENT_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_SERVER_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.SECURITY_SERVER_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    public static final PropertiesAttributeDefinition PROPERTIES =
            new PropertiesAttributeDefinition.Builder(JdkORBSubsystemConstants.PROPERTIES, true)
            .setAllowExpression(true)
            .build();



    // list that contains the orb attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_ATTRIBUTES = Arrays.asList(ORB_GIOP_VERSION, ORB_SOCKET_BINDING,
            ORB_SSL_SOCKET_BINDING, ORB_PERSISTENT_SERVER_ID);

    // list that contains the orb connection attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_TCP_ATTRIBUTES = Arrays.asList(ORB_TCP_HIGH_WATER_MARK,
            ORB_TCP_NUMBER_TO_RECLAIM_PROPERTY);

    // list that contains the orb initializer attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_INIT_ATTRIBUTES = Arrays.asList(ORB_INIT_SECURITY, ORB_INIT_TX);

    // list that contains the naming attribute definitions.
    static final List<SimpleAttributeDefinition> NAMING_ATTRIBUTES = Arrays.asList(NAMING_ROOT_CONTEXT,
            NAMING_EXPORT_CORBALOC);

    // list that contains the security attribute definitions.
    static final List<SimpleAttributeDefinition> SECURITY_ATTRIBUTES = Arrays.asList(SECURITY_SUPPORT_SSL,
            SECURITY_SECURITY_DOMAIN, SECURITY_ADD_COMPONENT_INTERCEPTOR, SECURITY_CLIENT_SUPPORTS,
            SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES);

    static final List<SimpleAttributeDefinition> SSL_CONFIG_ATTRIBUTES = Arrays.asList(SECURITY_CLIENT_SUPPORTS,
            SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES);

    // list that contains all attribute definitions.
    static final List<AttributeDefinition> SUBSYSTEM_ATTRIBUTES;

    // utility map that keys all definitions by their names.
    static final Map<String, AttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        SUBSYSTEM_ATTRIBUTES = new ArrayList<AttributeDefinition>();
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_TCP_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_INIT_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(NAMING_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(SECURITY_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.add(PROPERTIES);

        Map<String, AttributeDefinition> map = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition attribute : SUBSYSTEM_ATTRIBUTES) {
            map.put(attribute.getName(), attribute);
        }
        ATTRIBUTES_BY_NAME = map;
    }

    /**
     * <p>
     * Gets the {@code SimpleAttributeDefinition} identified by the specified name.
     * </p>
     *
     * @param attributeNAme a {@code String} representing the attribute name.
     * @return the corresponding attribute definition or {@code null} if no definition was found with that name.
     */
    public static AttributeDefinition valueOf(String attributeNAme) {
        return ATTRIBUTES_BY_NAME.get(attributeNAme);
    }

}
