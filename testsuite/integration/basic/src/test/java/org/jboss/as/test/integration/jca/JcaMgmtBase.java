/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

/**
 * Base class for JCA related tests
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class JcaMgmtBase extends ContainerResourceMgmtTestBase {


    protected static ModelNode subsystemAddress = new ModelNode().add(SUBSYSTEM, "jca");

    protected static ModelNode archiveValidationAddress = subsystemAddress.clone().add("archive-validation", "archive-validation");

    /**
     * Provide reload operation on server
     *
     * @throws Exception
     */
    public void reload() throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(getManagementClient(), 50000);
    }

    /**
     * Reads attribute from DMR model
     *
     * @param address       to read
     * @param attributeName
     * @return attribute value
     * @throws Exception
     */
    public ModelNode readAttribute(ModelNode address, String attributeName) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(OP_ADDR).set(address);
        return executeOperation(op);
    }

    /**
     * Writes attribute value
     *
     * @param address        to write
     * @param attributeName
     * @param attributeValue
     * @return result of operation
     * @throws Exception
     */
    public ModelNode writeAttribute(ModelNode address, String attributeName, String attributeValue) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(VALUE).set(attributeValue);
        op.get(OP_ADDR).set(address);
        return executeOperation(op);
    }

    /**
     * Set parameters for archive validation in JCA
     *
     * @param enabled    - if validation is enabled
     * @param failOnErr  - if validation should fail an error
     * @param failOnWarn - if validation should fail on error or warning
     * @throws Exception
     */
    public void setArchiveValidation(boolean enabled, boolean failOnErr, boolean failOnWarn) throws Exception {

        remove(archiveValidationAddress);
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(archiveValidationAddress);
        op.get("enabled").set(enabled);
        op.get("fail-on-error").set(failOnErr);
        op.get("fail-on-warn").set(failOnWarn);
        executeOperation(op);
        reload();
    }

    /**
     * Get some attribute from archive validation settings of server
     *
     * @param attributeName
     * @return boolean value of attribute
     * @throws Exception
     */
    public boolean getArchiveValidationAttribute(String attributeName) throws Exception {
        return readAttribute(archiveValidationAddress, attributeName).asBoolean();
    }

    /**
     * Executes operation operationName on node
     *
     * @param node
     * @param operationName
     * @return result of execution
     * @throws Exception
     */
    protected ModelNode executeOnNode(ModelNode node, String operationName) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        operation.get(OP_ADDR).set(node);
        return executeOperation(operation);
    }

    /**
     * Returns int value of statistics attribute
     *
     * @param attributeName
     * @param statisticNode - address of statistics node
     * @return int value of attribute
     * @throws Exception
     */
    protected int getStatisticsAttribute(String attributeName, ModelNode statisticNode) throws Exception {
        return readAttribute(statisticNode, attributeName).asInt();
    }


}
