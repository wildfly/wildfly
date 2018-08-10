/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class IIOPRootDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> IIOP_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.IIOP_CAPABILITY, false).build();

    static final ModelNode NONE = new ModelNode("none");

    static final ParameterValidator SSL_CONFIG_VALIDATOR = new EnumValidator<>(SSLConfigValue.class, true, false);

    static final StringLengthValidator LENGTH_VALIDATOR = new StringLengthValidator(1, Integer.MAX_VALUE, true, false);

    static final SensitivityClassification IIOP_SECURITY = new SensitivityClassification(IIOPExtension.SUBSYSTEM_NAME,
            "iiop-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition IIOP_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(
            IIOP_SECURITY);

    static final ParameterValidator VALIDATOR = new EnumValidator<>(IORTransportConfigValues.class,
            true, true);

    //ORB attributes

    protected static final AttributeDefinition PERSISTENT_SERVER_ID = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_PERSISTENT_SERVER_ID, ModelType.STRING, true).setAttributeGroup(Constants.ORB)
            .setDefaultValue(new ModelNode().set("1")).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    protected static final AttributeDefinition GIOP_VERSION = new SimpleAttributeDefinitionBuilder(Constants.ORB_GIOP_VERSION,
            ModelType.STRING, true).setAttributeGroup(Constants.ORB).setDefaultValue(new ModelNode().set("1.2"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    protected static final AttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_SOCKET_BINDING, ModelType.STRING, true).setAttributeGroup(Constants.ORB)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF).build();

    protected static final AttributeDefinition SSL_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_SSL_SOCKET_BINDING, ModelType.STRING, true).setAttributeGroup(Constants.ORB)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF).build();

    //TCP attributes
    protected static final AttributeDefinition HIGH_WATER_MARK = new SimpleAttributeDefinitionBuilder(
            Constants.TCP_HIGH_WATER_MARK, ModelType.INT, true).setAttributeGroup(Constants.ORB_TCP)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    protected static final AttributeDefinition NUMBER_TO_RECLAIM = new SimpleAttributeDefinitionBuilder(
            Constants.TCP_NUMBER_TO_RECLAIM, ModelType.INT, true).setAttributeGroup(Constants.ORB_TCP)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    //initializer attributes
    protected static final AttributeDefinition SECURITY = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_INIT_SECURITY, ModelType.STRING, true)
            .setAttributeGroup(Constants.ORB_INIT)
            .setDefaultValue(NONE)
            .setValidator(new EnumValidator<>(SecurityAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF).build();

    protected static final AttributeDefinition AUTHENTICATION_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_INIT_AUTH_CONTEXT, ModelType.STRING, true)
            .setAttributeGroup(Constants.ORB_INIT)
            .setValidator(LENGTH_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setCapabilityReference(Capabilities.AUTH_CONTEXT_CAPABILITY, IIOP_CAPABILITY)
            .addAccessConstraint(IIOP_SECURITY_DEF).build();

    protected static final AttributeDefinition TRANSACTIONS = new SimpleAttributeDefinitionBuilder(
            Constants.ORB_INIT_TRANSACTIONS, ModelType.STRING, true)
            .setAttributeGroup(Constants.ORB_INIT)
            .setDefaultValue(NONE)
            .setValidator(new EnumValidator<>(TransactionsAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    //Naming attributes
    protected static final AttributeDefinition ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setAttributeGroup(Constants.NAMING)
            .setDefaultValue(new ModelNode(Constants.ROOT_CONTEXT_INIT_REF))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    protected static final AttributeDefinition EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            Constants.NAMING_EXPORT_CORBALOC, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.NAMING)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    //Security attributes
    public static final AttributeDefinition SUPPORT_SSL = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SUPPORT_SSL, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SECURITY_DOMAIN, ModelType.STRING, true)
            .setAttributeGroup(Constants.SECURITY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(LENGTH_VALIDATOR)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setAlternatives(Constants.SERVER_SSL_CONTEXT, Constants.CLIENT_SSL_CONTEXT)
            .setCapabilityReference(Capabilities.LEGACY_SECURITY_DOMAIN_CAPABILITY, IIOP_CAPABILITY)
            .build();

    public static final AttributeDefinition SERVER_SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.SERVER_SSL_CONTEXT, ModelType.STRING, true)
            .setAttributeGroup(Constants.SECURITY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setValidator(LENGTH_VALIDATOR)
            .setAlternatives(Constants.SECURITY_SECURITY_DOMAIN)
            .setRequires(Constants.CLIENT_SSL_CONTEXT)
            .setCapabilityReference(Capabilities.SSL_CONTEXT_CAPABILITY, IIOP_CAPABILITY)
            .build();

    public static final AttributeDefinition CLIENT_SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.CLIENT_SSL_CONTEXT, ModelType.STRING, true)
            .setAttributeGroup(Constants.SECURITY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setValidator(LENGTH_VALIDATOR)
            .setAlternatives(Constants.SECURITY_SECURITY_DOMAIN)
            .setRequires(Constants.SERVER_SSL_CONTEXT)
            .setCapabilityReference(Capabilities.SSL_CONTEXT_CAPABILITY, IIOP_CAPABILITY)
            .build();

    @Deprecated
    public static final AttributeDefinition ADD_COMPONENT_INTERCEPTOR = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, ModelType.BOOLEAN, true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    public static final AttributeDefinition CLIENT_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_CLIENT_SUPPORTS, ModelType.STRING, true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    public static final AttributeDefinition CLIENT_REQUIRES = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_CLIENT_REQUIRES, ModelType.STRING, true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    public static final AttributeDefinition SERVER_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SERVER_SUPPORTS, ModelType.STRING, true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    public static final AttributeDefinition SERVER_REQUIRES = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SERVER_REQUIRES, ModelType.STRING, true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    public static final AttributeDefinition CLIENT_REQUIRES_SSL = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_CLIENT_REQUIRES_SSL, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(Boolean.FALSE))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition SERVER_REQUIRES_SSL = new SimpleAttributeDefinitionBuilder(
            Constants.SECURITY_SERVER_REQUIRES_SSL, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.SECURITY)
            .setDefaultValue(new ModelNode().set(Boolean.FALSE))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(IIOP_SECURITY_DEF)
            .build();

    public static final AttributeDefinition INTEROP_IONA = new SimpleAttributeDefinitionBuilder(
            Constants.INTEROP_IONA, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.INTEROP)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();


    protected static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder(
            Constants.PROPERTIES, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    //ior transport config attributes
    @Deprecated
    protected static final AttributeDefinition INTEGRITY = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_INTEGRITY, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(VALIDATOR)
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    protected static final AttributeDefinition CONFIDENTIALITY = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_CONFIDENTIALITY, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(VALIDATOR)
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    protected static final AttributeDefinition TRUST_IN_TARGET = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_TRUST_IN_TARGET, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(IORTransportConfigValues.class, true, true,
                    IORTransportConfigValues.NONE, IORTransportConfigValues.SUPPORTED))
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    protected static final AttributeDefinition TRUST_IN_CLIENT = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_TRUST_IN_CLIENT, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(VALIDATOR)
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    protected static final AttributeDefinition DETECT_REPLAY = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_DETECT_REPLAY, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(VALIDATOR)
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    @Deprecated
    protected static final AttributeDefinition DETECT_MISORDERING = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_TRANSPORT_DETECT_MISORDERING, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_TRANSPORT_CONFIG)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(VALIDATOR)
            .setAllowExpression(true)
            .setDeprecated(IIOPExtension.VERSION_1)
            .build();

    //ior as context attributes
    protected static final AttributeDefinition AUTH_METHOD = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_AS_CONTEXT_AUTH_METHOD, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_AS_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(AuthMethodValues.USERNAME_PASSWORD.toString()))
            .setValidator(new EnumValidator<>(AuthMethodValues.class, true, true))
            .setAllowExpression(true)
            .build();

    protected static final AttributeDefinition REALM = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_AS_CONTEXT_REALM, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_AS_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .setAllowExpression(true)
            .build();

    protected static final AttributeDefinition REQUIRED = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_AS_CONTEXT_REQUIRED, ModelType.BOOLEAN, true)
            .setAttributeGroup(Constants.IOR_AS_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();


    //ior sas context attributes
    protected static final AttributeDefinition CALLER_PROPAGATION = new SimpleAttributeDefinitionBuilder(
            Constants.IOR_SAS_CONTEXT_CALLER_PROPAGATION, ModelType.STRING, true)
            .setAttributeGroup(Constants.IOR_SAS_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(NONE)
            .setValidator(new EnumValidator<>(CallerPropagationValues.class, true, true))
            .setAllowExpression(true)
            .build();

    // list that contains ORB attribute definitions
    static final List<AttributeDefinition> ORB_ATTRIBUTES = Arrays.asList(PERSISTENT_SERVER_ID, GIOP_VERSION, SOCKET_BINDING,
            SSL_SOCKET_BINDING);

    // list that contains initializers attribute definitions
    static final List<AttributeDefinition> INITIALIZERS_ATTRIBUTES = Arrays.asList(SECURITY, AUTHENTICATION_CONTEXT, TRANSACTIONS);

    // list that contains naming attributes definitions
    static final List<AttributeDefinition> NAMING_ATTRIBUTES = Arrays.asList(ROOT_CONTEXT, EXPORT_CORBALOC);

    // list that contains security attributes definitions
    static final List<AttributeDefinition> SECURITY_ATTRIBUTES = Arrays.asList(SUPPORT_SSL, SECURITY_DOMAIN,
            SERVER_SSL_CONTEXT, CLIENT_SSL_CONTEXT, SERVER_REQUIRES_SSL, CLIENT_REQUIRES_SSL,
            ADD_COMPONENT_INTERCEPTOR, CLIENT_SUPPORTS, CLIENT_REQUIRES, SERVER_SUPPORTS, SERVER_REQUIRES);

    // list that contains interoperability attributes definitions
    static final List<AttributeDefinition> INTEROP_ATTRIBUTES = Arrays.asList(INTEROP_IONA);

    //list that contains tcp attributes definitions
    protected static final List<AttributeDefinition> TCP_ATTRIBUTES = Arrays.asList(HIGH_WATER_MARK,
            NUMBER_TO_RECLAIM);

    //list that contains ior sas attributes definitions
    static final List<AttributeDefinition> IOR_SAS_ATTRIBUTES = Arrays.asList(CALLER_PROPAGATION);

    //list that contains ior as attributes definitions
    static final List<AttributeDefinition> IOR_AS_ATTRIBUTES = Arrays.asList(AUTH_METHOD, REALM, REQUIRED);

    //list that contains ior transport config attributes definitions
    static final List<AttributeDefinition> IOR_TRANSPORT_CONFIG_ATTRIBUTES = Arrays.asList(INTEGRITY, CONFIDENTIALITY, TRUST_IN_TARGET,
            TRUST_IN_CLIENT, DETECT_REPLAY, DETECT_MISORDERING);

    static final List<AttributeDefinition> CONFIG_ATTRIBUTES = new ArrayList<>();
    static final List<AttributeDefinition> IOR_ATTRIBUTES = new ArrayList<>();
    static final List<AttributeDefinition> ALL_ATTRIBUTES = new ArrayList<>();
    static {
        CONFIG_ATTRIBUTES.addAll(ORB_ATTRIBUTES);
        CONFIG_ATTRIBUTES.addAll(TCP_ATTRIBUTES);
        CONFIG_ATTRIBUTES.addAll(INITIALIZERS_ATTRIBUTES);
        CONFIG_ATTRIBUTES.addAll(NAMING_ATTRIBUTES);
        CONFIG_ATTRIBUTES.addAll(SECURITY_ATTRIBUTES);
        CONFIG_ATTRIBUTES.addAll(INTEROP_ATTRIBUTES);
        CONFIG_ATTRIBUTES.add(PROPERTIES);

        IOR_ATTRIBUTES.addAll(IOR_TRANSPORT_CONFIG_ATTRIBUTES);
        IOR_ATTRIBUTES.addAll(IOR_AS_ATTRIBUTES);
        IOR_ATTRIBUTES.addAll(IOR_SAS_ATTRIBUTES);

        ALL_ATTRIBUTES.addAll(CONFIG_ATTRIBUTES);
        ALL_ATTRIBUTES.addAll(IOR_ATTRIBUTES);
    }

    public static final IIOPRootDefinition INSTANCE = new IIOPRootDefinition();

    private IIOPRootDefinition() {
        super(IIOPExtension.PATH_SUBSYSTEM, IIOPExtension.getResourceDescriptionResolver(), new IIOPSubsystemAdd(ALL_ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ALL_ATTRIBUTES;
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        super.registerCapabilities(resourceRegistration);
        resourceRegistration.registerCapability(IIOP_CAPABILITY);
    }

    private enum AuthMethodValues {

        NONE("none"), USERNAME_PASSWORD("username_password");

        private String name;

        AuthMethodValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum CallerPropagationValues {

        NONE("none"), SUPPORTED("supported");

        private String name;

        CallerPropagationValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum IORTransportConfigValues {

        NONE("none"), SUPPORTED("supported"), REQUIRED("required");

        private String name;

        IORTransportConfigValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}
