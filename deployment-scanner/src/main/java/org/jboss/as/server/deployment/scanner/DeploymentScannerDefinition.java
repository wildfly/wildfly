package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 25.1.12 17:24
 */
public class DeploymentScannerDefinition extends SimpleResourceDefinition {

    public static final DeploymentScannerDefinition INSTANCE = new DeploymentScannerDefinition();

    private DeploymentScannerDefinition() {
        super(DeploymentScannerExtension.SCANNERS_PATH,
                DeploymentScannerExtension.getResourceDescriptionResolver("deployment.scanner"),
                DeploymentScannerAdd.INSTANCE, DeploymentScannerRemove.INSTANCE
        );
    }

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.NAME, ModelType.STRING, false)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1))
                    .setDefaultValue(new ModelNode().set(DeploymentScannerExtension.DEFAULT_SCANNER_NAME))
                    .build();

    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.PATH, ModelType.STRING, false)
                    .setXmlName(Attribute.PATH.getLocalName())
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();
    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.RELATIVE_TO, ModelType.STRING, true)
                    .setXmlName(Attribute.RELATIVE_TO.getLocalName())
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
                    .build();
    protected static final SimpleAttributeDefinition SCAN_ENABLED =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.SCAN_ENABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.SCAN_ENABLED.getLocalName())
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition SCAN_INTERVAL =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.SCAN_INTERVAL, ModelType.INT, true)
                    .setXmlName(Attribute.SCAN_INTERVAL.getLocalName())
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(5000))
                    .build();
    protected static final SimpleAttributeDefinition AUTO_DEPLOY_ZIPPED =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTO_DEPLOY_ZIPPED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.AUTO_DEPLOY_ZIPPED.getLocalName())
                    .setDefaultValue(new ModelNode().set(true))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition AUTO_DEPLOY_EXPLODED =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTO_DEPLOY_EXPLODED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.AUTO_DEPLOY_EXPLODED.getLocalName())
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();

    protected static final SimpleAttributeDefinition AUTO_DEPLOY_XML =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTO_DEPLOY_XML, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.AUTO_DEPLOY_XML.getLocalName())
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();

    protected static final SimpleAttributeDefinition DEPLOYMENT_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEPLOYMENT_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.DEPLOYMENT_TIMEOUT.getLocalName())
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(60))
                    .build();
    protected static final SimpleAttributeDefinition[] ALL_ATTRIBUTES = {PATH,RELATIVE_TO,SCAN_ENABLED,SCAN_INTERVAL,AUTO_DEPLOY_EXPLODED,AUTO_DEPLOY_XML,AUTO_DEPLOY_ZIPPED,DEPLOYMENT_TIMEOUT};

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //resourceRegistration.registerReadOnlyAttribute(NAME, null);
        resourceRegistration.registerReadWriteAttribute(PATH, null, new ReloadRequiredWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler());
        resourceRegistration.registerReadWriteAttribute(SCAN_ENABLED, null, WriteEnabledAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(SCAN_INTERVAL, null, WriteScanIntervalAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_ZIPPED, null, WriteAutoDeployZipAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_EXPLODED, null, WriteAutoDeployExplodedAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_XML, null, WriteAutoDeployXMLAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEPLOYMENT_TIMEOUT, null, WriteDeploymentTimeoutAttributeHandler.INSTANCE);
    }
}
