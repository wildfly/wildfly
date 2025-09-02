/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.mgmt.resourceadapter;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.checkModelParams;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.raAdminProperties;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.raCommonProperties;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.raConnectionProperties;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.setOperationParams;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.jca.ConnectionSecurityType;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Resource adapter operation unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author Flavia Rainone
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ResourceAdapterOperationsUnitTestCase extends ContainerResourceMgmtTestBase {
    private static final Deque<ModelNode> REMOVE_ADDRESSES = new LinkedList<>();

    private static final ModelNode RAR_ADDRESS;
    static {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.add("resource-adapter", "some.rar");
        address.protect();
        RAR_ADDRESS = address;
    }

    @BeforeAll
    public static void configureElytron() throws Exception {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            addAuth(client, "AuthCtxt");
            addAuth(client, "AuthCtxtAndApp");
        }
    }

    @AfterAll
    public static void removeElytronConfig() throws Exception {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            ModelNode address;
            while ((address = REMOVE_ADDRESSES.pollFirst()) != null) {
                execute(client, Operations.createRemoveOperation(address));
            }
        }
    }

    @AfterEach
    public void removeRar() throws IOException {
        // Don't let failure in one test leave cruft behind to break the rest
        try {
            remove(RAR_ADDRESS);
        } catch (MgmtOperationException ignored) {
            // ignore -- assume it's the usual case where the test method already removed this
        }
    }

    @Test
    public void addComplexResourceAdapterWithAppSecurity() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.APPLICATION, null);
    }

    @Test
    public void addComplexResourceAdapterWithAppSecurity_UserPassRecovery() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT_AND_APPLICATION, ConnectionSecurityType.USER_PASSWORD);
    }

    @Test
    public void addComplexResourceAdapterWithAppSecurity_ElytronRecovery() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT_AND_APPLICATION, ConnectionSecurityType.ELYTRON);
    }

    @Test
    public void addComplexResourceAdapterWithAppSecurity_ElytronAuthCtxtRecovery() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT_AND_APPLICATION, ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT);
    }

    @Test
    public void addComplexResourceAdapterWithElytron() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON, ConnectionSecurityType.ELYTRON);
    }

    @Test
    public void addComplexResourceAdapterWithElytron_NoRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON, null);
    }

    @Test
    public void addComplexResourceAdapterWithElytron_UserPassRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON, ConnectionSecurityType.USER_PASSWORD);
    }

    @Test
    public void addComplexResourceAdapterWithElytron_ElytronAuthCtxtRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON, ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT);
    }

    @Test
    public void addComplexResourceAdapterWithElytronAuthCtxt() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT,
                ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT);
    }

    @Test
    public void addComplexResourceAdapterWithElytronAuthCtxtN_oRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT, null);
    }

    @Test
    public void addComplexResourceAdapterWithElytronAuthCtxt_UserPassRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT, ConnectionSecurityType.USER_PASSWORD);
    }

    @Test
    public void addComplexResourceAdapterWithElytronAuthCtxt_ElytronRecoverySec() throws Exception {
        complexResourceAdapterAddTest(ConnectionSecurityType.ELYTRON_AUTHENTICATION_CONTEXT, ConnectionSecurityType.ELYTRON);
    }

    private void complexResourceAdapterAddTest(ConnectionSecurityType connectionSecurityType,
            ConnectionSecurityType connectionRecoverySecurityType) throws Exception {
        final ModelNode address = RAR_ADDRESS;

        Properties params = raCommonProperties();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        setOperationParams(operation, params);
        operation.get("beanvalidationgroups").add("Class0");
        operation.get("beanvalidationgroups").add("Class00");
        executeOperation(operation);

        final ModelNode address1 = address.clone();
        address1.add("config-properties", "Property");
        address1.protect();

        final ModelNode operation11 = new ModelNode();
        operation11.get(OP).set("add");
        operation11.get(OP_ADDR).set(address1);
        operation11.get("value").set("A");

        executeOperation(operation11);

        final ModelNode conAddress = address.clone();
        conAddress.add("connection-definitions", "Pool1");
        conAddress.protect();

        Properties conParams = raConnectionProperties(connectionSecurityType, connectionRecoverySecurityType);

        final ModelNode operation2 = new ModelNode();
        operation2.get(OP).set("add");
        operation2.get(OP_ADDR).set(conAddress);
        setOperationParams(operation2, conParams);

        executeOperation(operation2);

        final ModelNode con1Address = conAddress.clone();
        con1Address.add("config-properties", "Property");
        con1Address.protect();

        final ModelNode operation21 = new ModelNode();
        operation21.get(OP).set("add");
        operation21.get(OP_ADDR).set(con1Address);
        operation21.get("value").set("B");

        executeOperation(operation21);

        final ModelNode admAddress = address.clone();
        admAddress.add("admin-objects", "Pool2");
        admAddress.protect();

        Properties admParams = raAdminProperties();

        final ModelNode operation3 = new ModelNode();
        operation3.get(OP).set("add");
        operation3.get(OP_ADDR).set(admAddress);
        setOperationParams(operation3, admParams);

        executeOperation(operation3);

        final ModelNode adm1Address = admAddress.clone();
        adm1Address.add("config-properties", "Property");
        adm1Address.protect();

        final ModelNode operation31 = new ModelNode();
        operation31.get(OP).set("add");
        operation31.get(OP_ADDR).set(adm1Address);
        operation31.get("value").set("D");

        executeOperation(operation31);

        List<ModelNode> newList = marshalAndReparseRaResources("resource-adapter");

        remove(address);

        Assertions.assertNotNull(newList);

        ModelNode node = findNodeWithProperty(newList, "archive", "some.rar");
        Assertions.assertNotNull(node, "There is no archive element:" + newList);
        Assertions.assertTrue(checkModelParams(node,params),"compare failed, node:"+node.asString()+"\nparams:"+params);
        Assertions.assertEquals("[\"Class0\",\"Class00\"]", node.get("beanvalidationgroups").asString(), "beanvalidationgroups element is incorrect:" + node.get("beanvalidationgroups").asString());

        node = findNodeWithProperty(newList, "jndi-name", "java:jboss/name1");
        Assertions.assertNotNull(node, "There is no connection jndi-name element:" + newList);
        Assertions.assertTrue(checkModelParams(node,conParams),"compare failed, node:"+node.asString()+"\nparams:"+conParams);

        node = findNodeWithProperty(newList, "jndi-name", "java:jboss/Name3");
        Assertions.assertNotNull(node, "There is no admin jndi-name element:" + newList);
        Assertions.assertTrue(checkModelParams(node, admParams),
                "compare failed, node:" + node.asString() + "\nparams:" + admParams);

        node = findNodeWithProperty(newList, "value", "D");
        Assertions.assertNotNull(node, "There is no admin-object config-property element:" + newList);

        Map<String, ModelNode> parseChildren = getChildren(node.get("address"));
        Assertions.assertEquals("Pool2", parseChildren.get("admin-objects").asString());
        Assertions.assertEquals("Property", parseChildren.get("config-properties").asString());

        node = findNodeWithProperty(newList, "value", "A");
        Assertions.assertNotNull(node, "There is no resource-adapter config-property element:" + newList);

        parseChildren = getChildren(node.get("address"));
        Assertions.assertEquals("some.rar", parseChildren.get("resource-adapter").asString());
        Assertions.assertEquals("Property", parseChildren.get("config-properties").asString());

        node = findNodeWithProperty(newList, "value", "B");
        Assertions.assertNotNull(node, "There is no connection config-property element:" + newList);

        parseChildren = getChildren(node.get("address"));
        Assertions.assertEquals("Pool1", parseChildren.get("connection-definitions").asString());
        Assertions.assertEquals("Property", parseChildren.get("config-properties").asString());
    }

    public List<ModelNode> marshalAndReparseRaResources(final String childType) throws Exception {
        ResourceAdapterSubsystemParser parser = new ResourceAdapterSubsystemParser();
        return xmlToModelOperations(modelToXml("resource-adapters", childType, parser), Namespace.CURRENT.getUriString(),
                parser);
    }

    private static void addAuth(final ModelControllerClient client, final String name) throws IOException {
        ModelNode address = Operations.createAddress("subsystem", "elytron", "authentication-configuration", name);
        REMOVE_ADDRESSES.addLast(address.clone());
        ModelNode op = Operations.createAddOperation(address);
        op.get("security-domain").set("ApplicationDomain");
        final ModelNode cr = op.get("credential-reference").setEmptyObject();
        cr.get("clear-text").set("value");
        execute(client, op);

        address = Operations.createAddress("subsystem", "elytron", "authentication-context", name);
        REMOVE_ADDRESSES.addFirst(address.clone());
        op = Operations.createAddOperation(address);
        final ModelNode mr = op.get("match-rules").setEmptyList();
        final ModelNode ac = new ModelNode().setEmptyObject();
        ac.get("authentication-configuration").set(name);
        mr.add(ac);
        execute(client, op);
    }

    private static void execute(final ModelControllerClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException(Operations.getFailureDescription(result).asString());
        }
    }
}
