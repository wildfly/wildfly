package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 22.2.12 15:03
 */
public class WebConnectorDefinition extends SimpleResourceDefinition {
    protected static final WebConnectorDefinition INSTANCE = new WebConnectorDefinition();


    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
                    .setXmlName(Constants.NAME)
                    .setAllowNull(true) // todo should be false, but 'add' won't validate then
                    .build();

    protected static final SimpleAttributeDefinition PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL, ModelType.STRING)
                    .setXmlName(Constants.PROTOCOL)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
                    .setXmlName(Constants.SOCKET_BINDING)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .build();

    protected static final SimpleAttributeDefinition SCHEME =
            new SimpleAttributeDefinitionBuilder(Constants.SCHEME, ModelType.STRING)
                    .setXmlName(Constants.SCHEME)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    //.setDefaultValue(new ModelNode("http"))
                    .build();

    protected static final SimpleAttributeDefinition EXECUTOR =
            new SimpleAttributeDefinitionBuilder(Constants.EXECUTOR, ModelType.STRING)
                    .setXmlName(Constants.EXECUTOR)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();


    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
                    .setXmlName(Constants.ENABLED)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition ENABLE_LOOKUPS =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLE_LOOKUPS, ModelType.BOOLEAN)
                    .setXmlName(Constants.ENABLE_LOOKUPS)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition PROXY_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_NAME, ModelType.STRING)
                    .setXmlName(Constants.PROXY_NAME)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition PROXY_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_PORT, ModelType.INT)
                    .setXmlName(Constants.PROXY_PORT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition MAX_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_POST_SIZE, ModelType.INT)
                    .setXmlName(Constants.MAX_POST_SIZE)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(2097152))
                    .build();

    protected static final SimpleAttributeDefinition MAX_SAVE_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_SAVE_POST_SIZE, ModelType.INT)
                    .setXmlName(Constants.MAX_SAVE_POST_SIZE)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(4096))
                    .build();

    protected static final SimpleAttributeDefinition SECURE =
            new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN)
                    .setXmlName(Constants.SECURE)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition REDIRECT_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_PORT, ModelType.INT)
                    .setXmlName(Constants.REDIRECT_PORT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(8433))
                    .build();

    protected static final SimpleAttributeDefinition MAX_CONNECTIONS =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_CONNECTIONS, ModelType.INT) //todo why is this string somewhere??
                    .setXmlName(Constants.MAX_CONNECTIONS)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                            //.setDefaultValue(new ModelNode(8433))
                    .build();

    protected static final SimpleListAttributeDefinition VIRTUAL_SERVER =
                SimpleListAttributeDefinition.Builder.of(Constants.VIRTUAL_SERVER,
                        new SimpleAttributeDefinitionBuilder(Constants.VIRTUAL_SERVER, ModelType.STRING, false)
                                .setXmlName(Constants.VIRTUAL_SERVER)
                                .setAllowNull(false)
                                .setValidator(new StringLengthValidator(1, false))
                                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                .build())
                        .setAllowNull(true)
                        .build();

    protected static final SimpleAttributeDefinition[] CONNECTOR_ATTRIBUTES = {
            //NAME, // name is read-only
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PROTOCOL,
            SCHEME,
            SOCKET_BINDING,
            ENABLE_LOOKUPS,
            PROXY_NAME,
            PROXY_PORT,
            REDIRECT_PORT,
            SECURE,
            MAX_POST_SIZE,
            MAX_SAVE_POST_SIZE,
            ENABLED,
            EXECUTOR,
            MAX_CONNECTIONS

    };

    private WebConnectorDefinition() {
        super(WebExtension.CONNECTOR_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONNECTOR),
                WebConnectorAdd.INSTANCE,
                WebConnectorRemove.INSTANCE);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration connectors) {
        connectors.registerReadOnlyAttribute(NAME, null);
        for (AttributeDefinition def : CONNECTOR_ATTRIBUTES) {
            connectors.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
        connectors.registerReadWriteAttribute(VIRTUAL_SERVER,null,new ReloadRequiredWriteAttributeHandler(VIRTUAL_SERVER));

        for (final SimpleAttributeDefinition def : WebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(def, WebConnectorMetrics.INSTANCE);
        }
    }
}
