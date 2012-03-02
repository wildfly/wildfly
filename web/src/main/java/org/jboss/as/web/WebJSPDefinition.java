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
 * @created 23.2.12 18:47
 */
public class WebJSPDefinition extends SimpleResourceDefinition {
    public static final WebJSPDefinition INSTANCE = new WebJSPDefinition();

    protected static final SimpleAttributeDefinition DEVELOPMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DEVELOPMENT, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DEVELOPMENT)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition DISABLED =
            new SimpleAttributeDefinitionBuilder(Constants.DISABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DISABLED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition KEEP_GENERATED =
            new SimpleAttributeDefinitionBuilder(Constants.KEEP_GENERATED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.KEEP_GENERATED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition TRIM_SPACES =
            new SimpleAttributeDefinitionBuilder(Constants.TRIM_SPACES, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.TRIM_SPACES)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition TAG_POOLING =
            new SimpleAttributeDefinitionBuilder(Constants.TAG_POOLING, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.TAG_POOLING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition MAPPED_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.MAPPED_FILE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.MAPPED_FILE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition CHECK_INTERVAL =
            new SimpleAttributeDefinitionBuilder(Constants.CHECK_INTERVAL, ModelType.INT, true)
                    .setXmlName(Constants.CHECK_INTERVAL)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(0, true))
                    .setDefaultValue(new ModelNode(0))
                    .build();

    protected static final SimpleAttributeDefinition MODIFICATION_TEST_INTERVAL =
            new SimpleAttributeDefinitionBuilder(Constants.MODIFICATION_TEST_INTERVAL, ModelType.INT, true)
                    .setXmlName(Constants.MODIFICATION_TEST_INTERVAL)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(0, true))
                    .setDefaultValue(new ModelNode(4))
                    .build();
    protected static final SimpleAttributeDefinition RECOMPILE_ON_FAIL =
            new SimpleAttributeDefinitionBuilder(Constants.RECOMPILE_ON_FAIL, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.RECOMPILE_ON_FAIL)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.SMAP, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.SMAP)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition DUMP_SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.DUMP_SMAP, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DUMP_SMAP)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition GENERATE_STRINGS_AS_CHAR_ARRAYS =
            new SimpleAttributeDefinitionBuilder(Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();


    protected static final SimpleAttributeDefinition ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition SCRATCH_DIR =
            new SimpleAttributeDefinitionBuilder(Constants.SCRATCH_DIR, ModelType.STRING, true)
                    .setXmlName(Constants.SCRATCH_DIR)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition SOURCE_VM =
            new SimpleAttributeDefinitionBuilder(Constants.SOURCE_VM, ModelType.STRING, true)
                    .setXmlName(Constants.SOURCE_VM)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("1.5"))
                    .build();
    protected static final SimpleAttributeDefinition TARGET_VM =
            new SimpleAttributeDefinitionBuilder(Constants.TARGET_VM, ModelType.STRING, true)
                    .setXmlName(Constants.TARGET_VM)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("1.5"))
                    .build();


    protected static final SimpleAttributeDefinition JAVA_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.JAVA_ENCODING, ModelType.STRING, true)
                    .setXmlName(Constants.JAVA_ENCODING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("UTF8"))
                    .build();


    protected static final SimpleAttributeDefinition X_POWERED_BY =
            new SimpleAttributeDefinitionBuilder(Constants.X_POWERED_BY, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.X_POWERED_BY)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition DISPLAY_SOURCE_FRAGMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DISPLAY_SOURCE_FRAGMENT, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DISPLAY_SOURCE_FRAGMENT)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN))
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition[] JSP_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            DEVELOPMENT,
            DISABLED,
            KEEP_GENERATED,
            TRIM_SPACES,
            TAG_POOLING,
            MAPPED_FILE,
            CHECK_INTERVAL,
            MODIFICATION_TEST_INTERVAL,
            RECOMPILE_ON_FAIL,
            SMAP,
            DUMP_SMAP,
            GENERATE_STRINGS_AS_CHAR_ARRAYS,
            ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE,
            SCRATCH_DIR,
            SOURCE_VM,
            TARGET_VM,
            JAVA_ENCODING,
            X_POWERED_BY,
            DISPLAY_SOURCE_FRAGMENT
    };

    private WebJSPDefinition() {
        super(WebExtension.JSP_CONFIGURATION_PATH,
                WebExtension.getResourceDescriptionResolver("configuration.jsp"),
                WebJSPConfigurationAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration jsp) {
        for (SimpleAttributeDefinition def : JSP_ATTRIBUTES) {
            jsp.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }
}
