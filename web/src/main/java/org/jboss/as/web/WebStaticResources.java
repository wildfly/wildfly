package org.jboss.as.web;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 24.2.12 12:26
 */
public class WebStaticResources extends SimpleResourceDefinition {
    public static final WebStaticResources INSTANCE = new WebStaticResources();

    protected static final SimpleAttributeDefinition LISTINGS =
            new SimpleAttributeDefinitionBuilder(Constants.LISTINGS, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.LISTINGS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition SENDFILE =
            new SimpleAttributeDefinitionBuilder(Constants.SENDFILE, ModelType.INT, true)
                    .setXmlName(Constants.SENDFILE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(1, true))
                    .setDefaultValue(new ModelNode(49152))
                    .build();
    protected static final SimpleAttributeDefinition FILE_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.FILE_ENCODING, ModelType.STRING, true)
                    .setXmlName(Constants.FILE_ENCODING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition READ_ONLY =
            new SimpleAttributeDefinitionBuilder(Constants.READ_ONLY, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.READ_ONLY)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition WEBDAV =
            new SimpleAttributeDefinitionBuilder(Constants.WEBDAV, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.WEBDAV)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition SECRET =
            new SimpleAttributeDefinitionBuilder(Constants.SECRET, ModelType.STRING, true)
                    .setXmlName(Constants.SECRET)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();
    protected static final SimpleAttributeDefinition MAX_DEPTH =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_DEPTH, ModelType.INT, true)
                    .setXmlName(Constants.MAX_DEPTH)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(1, true))
                    .setDefaultValue(new ModelNode(3))
                    .build();

    protected static final SimpleAttributeDefinition DISABLED =
            new SimpleAttributeDefinitionBuilder(Constants.DISABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DISABLED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition[] STATIC_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            LISTINGS,
            SENDFILE,
            FILE_ENCODING,
            READ_ONLY,
            WEBDAV,
            SECRET,
            MAX_DEPTH,
            DISABLED
    };

    private WebStaticResources() {
        super(WebExtension.STATIC_RESOURCES_PATH,
                WebExtension.getResourceDescriptionResolver("configuration.static"),
                WebStaticResourcesAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resources) {
        for (SimpleAttributeDefinition def : STATIC_ATTRIBUTES) {
            resources.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }

    }
}
