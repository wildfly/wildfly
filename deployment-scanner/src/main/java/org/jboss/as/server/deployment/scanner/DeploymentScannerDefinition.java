/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 25.1.12 17:24
 */
public class DeploymentScannerDefinition extends SimpleResourceDefinition {

    DeploymentScannerDefinition(final PathManager pathManager) {
        super(DeploymentScannerExtension.SCANNERS_PATH,
                DeploymentScannerExtension.getResourceDescriptionResolver("deployment.scanner"),
                new DeploymentScannerAdd(pathManager), DeploymentScannerRemove.INSTANCE
        );
    }

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.NAME, ModelType.STRING, false)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
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
                    .setDefaultValue(new ModelNode().set(0))
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
                    .setDefaultValue(new ModelNode().set(600))
                    .build();
    protected static final SimpleAttributeDefinition[] ALL_ATTRIBUTES = {PATH,RELATIVE_TO,SCAN_ENABLED,SCAN_INTERVAL,AUTO_DEPLOY_EXPLODED,AUTO_DEPLOY_XML,AUTO_DEPLOY_ZIPPED,DEPLOYMENT_TIMEOUT};

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //resourceRegistration.registerReadOnlyAttribute(NAME, null);
        resourceRegistration.registerReadWriteAttribute(PATH, null, new ReloadRequiredWriteAttributeHandler(PATH));
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler(RELATIVE_TO));
        resourceRegistration.registerReadWriteAttribute(SCAN_ENABLED, null, WriteEnabledAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(SCAN_INTERVAL, null, WriteScanIntervalAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_ZIPPED, null, WriteAutoDeployZipAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_EXPLODED, null, WriteAutoDeployExplodedAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(AUTO_DEPLOY_XML, null, WriteAutoDeployXMLAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEPLOYMENT_TIMEOUT, null, WriteDeploymentTimeoutAttributeHandler.INSTANCE);
    }
}
