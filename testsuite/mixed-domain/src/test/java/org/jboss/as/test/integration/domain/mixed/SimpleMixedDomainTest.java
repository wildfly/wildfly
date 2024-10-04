/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLONE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_PROFILE;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class SimpleMixedDomainTest  {

    private static final String ACTIVE_PROFILE = "full-ha";
    MixedDomainTestSupport support;
    Version.AsVersion version;

    @Before
    public void init() throws Exception {
        support = MixedDomainTestSuite.getSupport(this.getClass());
        version = MixedDomainTestSuite.getVersion(this.getClass());
    }

    @AfterClass
    public static synchronized void afterClass() {
        MixedDomainTestSuite.afterClass();
    }

    @Test
    public void test00001_ServerRunning() throws Exception {
        URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(DomainTestSupport.secondaryAddress) + ":8080").openConnection();
        connection.connect();
    }

    @Test
    public void test00002_Versioning() throws Exception {
        DomainClient primaryClient = support.getDomainPrimaryLifecycleUtil().createDomainClient();
        ModelNode primaryModel;
        try {
            primaryModel = readDomainModelForVersions(primaryClient);
        } finally {
            IoUtils.safeClose(primaryClient);
        }
        DomainClient secondaryClient = support.getDomainSecondaryLifecycleUtil().createDomainClient();
        ModelNode secondaryModel;
        try {
            secondaryModel = readDomainModelForVersions(secondaryClient);
        } finally {
            IoUtils.safeClose(secondaryClient);
        }

        cleanupKnownDifferencesInModelsForVersioningCheck(primaryModel, secondaryModel);

        //The version fields should be the same
        assertEquals(primaryModel, secondaryModel);
    }

    @Test
    public void test00010_JgroupsTransformers() throws Exception {
        final DomainClient primaryClient = support.getDomainPrimaryLifecycleUtil().createDomainClient();
        try {
            // Check composite operation
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            compositeOp.get(STEPS).add(createProtocolPutPropertyOperation("tcp", "MPING", "send_on_all_interfaces", "true"));
            compositeOp.get(STEPS).add(createProtocolPutPropertyOperation("tcp", "MPING", "receive_on_all_interfaces", "true"));

            DomainTestUtils.executeForResult(compositeOp, primaryClient);
        } finally {
            IoUtils.safeClose(primaryClient);
        }
    }


    /**
     * Tests test-connection-in-pool() of ExampleDS.
     *
     * @throws Exception
     */
    @Test
    public void test00011_ExampleDSConnection() throws Exception{
        PathAddress exampleDSAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, "secondary"),
                PathElement.pathElement(RUNNING_SERVER, "server-one"), PathElement.pathElement(SUBSYSTEM, "datasources"),
                PathElement.pathElement("data-source", "ExampleDS"));
        DomainClient primaryClient = support.getDomainPrimaryLifecycleUtil().createDomainClient();
        try {
            ModelNode op = Util.createOperation("test-connection-in-pool", PathAddress.pathAddress(exampleDSAddress));
            ModelNode response = primaryClient.execute(op);
            assertEquals(op.toString() + '\n' + response.toString(), SUCCESS, response.get(OUTCOME).asString());
        } finally {
            IoUtils.safeClose(primaryClient);
        }
    }

    //Do this one last since it changes the host model of the secondary hosts
    @Test
    public void test99999_ProfileClone() throws Exception {
        profileCloneEap();
    }

    private void profileCloneEap() throws Exception {
        final DomainClient primaryClient = support.getDomainPrimaryLifecycleUtil().createDomainClient();
        final DomainClient secondaryClient = support.getDomainSecondaryLifecycleUtil().createDomainClient();
        try {
            final PathAddress newProfileAddress = PathAddress.pathAddress(PROFILE, "new-profile");

            //Create a new profile (so that we can ignore it on the host later)
            DomainTestUtils.executeForResult(Util.createAddOperation(newProfileAddress), primaryClient);

            //Attempt to clone it. It should work but not exist on the secondary since unused configuration is ignored
            final ModelNode clone = Util.createEmptyOperation(CLONE, newProfileAddress);
            clone.get(TO_PROFILE).set("cloned");
            DomainTestUtils.executeForResult(clone, primaryClient);

            //Check the new profile does not exist on the secondary
            final ModelNode readChildrenNames = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
            readChildrenNames.get(CHILD_TYPE).set(PROFILE);
            ModelNode result = DomainTestUtils.executeForResult(readChildrenNames, secondaryClient);
            List<ModelNode> list = result.asList();
            Assert.assertEquals(1, list.size());
            Assert.assertEquals(list.toString(), "full-ha", list.get(0).asString());

            //Update the server group to use the new profile
            DomainTestUtils.executeForResult(
                    Util.getWriteAttributeOperation(PathAddress.pathAddress(SERVER_GROUP, "other-server-group"), PROFILE, "new-profile"),
                    primaryClient);

            //Check the profiles
            result = DomainTestUtils.executeForResult(readChildrenNames, secondaryClient);
            list = result.asList();
            Assert.assertEquals(1, list.size());
            Assert.assertEquals(list.toString(), "new-profile", list.get(0).asString());

        } finally {
            IoUtils.safeClose(secondaryClient);
            IoUtils.safeClose(primaryClient);
        }
    }
    /*
        !!!!!!!!! ADD TESTS IN NUMERICAL ORDER !!!!!!!!!!
        Please observe the test<5 digits>_ pattern for the names to ensure the order
     */


    private static ModelNode createProtocolPutPropertyOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(PROFILE, ACTIVE_PROFILE))
                .append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "jgroups"))
                .append(PathElement.pathElement("stack", stackName))
                .append(PathElement.pathElement("protocol", protocolName));

        ModelNode operation = Util.createOperation(MapOperations.MAP_PUT_DEFINITION, address);
        operation.get(ModelDescriptionConstants.NAME).set("properties");
        operation.get("key").set(propertyName);
        operation.get(ModelDescriptionConstants.VALUE).set(propertyValue);

        return operation;
    }

    private void cleanupKnownDifferencesInModelsForVersioningCheck(ModelNode primaryModel, ModelNode secondaryModel) {
        //First get rid of any undefined crap
        cleanUndefinedNodes(primaryModel);
        cleanUndefinedNodes(secondaryModel);
    }

    private void cleanUndefinedNodes(ModelNode model) {
        Set<String> removals = new HashSet<String>();
        for (String key : model.keys()) {
            if (!model.hasDefined(key)) {
                removals.add(key);
            }
        }
        for (String key : removals) {
            model.remove(key);
        }
    }

    private ModelNode readDomainModelForVersions(DomainClient domainClient) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).setEmptyList();
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_RUNTIME).set(false);
        op.get(PROXIES).set(false);

        ModelNode model = DomainTestUtils.executeForResult(op, domainClient);

        model.remove(EXTENSION);
        model.remove(HOST);
        model.remove(INTERFACE);
        model.remove(MANAGEMENT_CLIENT_CONTENT);
        model.remove(PROFILE);
        model.remove(SERVER_GROUP);
        model.remove(SOCKET_BINDING_GROUP);
        model.remove(SYSTEM_PROPERTY);
        model.remove(CORE_SERVICE);

        return model;
    }
}
