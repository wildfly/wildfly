/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.security.Constants;
import org.jboss.as.security.SecurityExtension;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
    public synchronized static void afterClass() {
        MixedDomainTestSuite.afterClass();
    }

    @Test
    public void test1ServerRunning() throws Exception {
        URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(DomainTestSupport.slaveAddress) + ":8080").openConnection();
        connection.connect();
    }

    @Test
    public void test2Versioning() throws Exception {
        if (version == Version.AsVersion.EAP_6_2_0) {
            //6.2.0 has the slave report back its own version, rather than the one from the DC,
            //which is what happens in more recent slaves
            return;
        }

        DomainClient masterClient = support.getDomainMasterLifecycleUtil().createDomainClient();
        ModelNode masterModel;
        try {
            masterModel = readDomainModelForVersions(masterClient);
        } finally {
            IoUtils.safeClose(masterClient);
        }
        DomainClient slaveClient = support.getDomainSlaveLifecycleUtil().createDomainClient();
        ModelNode slaveModel;
        try {
            slaveModel = readDomainModelForVersions(slaveClient);
        } finally {
            IoUtils.safeClose(slaveClient);
        }

        cleanupKnownDifferencesInModelsForVersioningCheck(masterModel, slaveModel);
        //The version fields should be the same
        assertEquals(masterModel, slaveModel);
    }

    @Test
    public void test9SecurityTransformers() throws Exception {
        final DomainClient masterClient = support.getDomainMasterLifecycleUtil().createDomainClient();
        final DomainClient slaveClient = support.getDomainSlaveLifecycleUtil().createDomainClient();
        try {
            PathAddress  subsystem = PathAddress.pathAddress(PathElement.pathElement(PROFILE, ACTIVE_PROFILE), PathElement.pathElement(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME));
            PathAddress webPolicy = subsystem.append(Constants.SECURITY_DOMAIN, "jboss-web-policy").append(Constants.AUTHORIZATION, Constants.CLASSIC);
            ModelNode options = new ModelNode();
            options.add("a", "b");
            ModelNode op = Util.getWriteAttributeOperation(webPolicy.append(Constants.POLICY_MODULE, "Delegating"), Constants.MODULE_OPTIONS, options);
            DomainTestUtils.executeForResult(op, masterClient);

            //TODO check the resources
            //System.out.println(DomainTestUtils.executeForResult(Util.createOperation(READ_RESOURCE_OPERATION, address), modelControllerClient));

            PathAddress jaspi = subsystem.append(Constants.SECURITY_DOMAIN, "jaspi-test").append(Constants.AUTHENTICATION, Constants.JASPI);

            PathAddress jaspiLoginStack = jaspi.append(Constants.LOGIN_MODULE_STACK, "lm-stack");
            op = Util.createAddOperation(jaspiLoginStack.append(Constants.LOGIN_MODULE, "test2"));
            op.get(Constants.CODE).set("UserRoles");
            op.get(Constants.FLAG).set("required");
            op.get(Constants.MODULE).set("test-jaspi");
            DomainTestUtils.executeForResult(op, masterClient);

            op = Util.createAddOperation(jaspi.append(Constants.AUTH_MODULE, "Delegating"));
            op.get(Constants.CODE).set("Delegating");
            op.get(Constants.LOGIN_MODULE_STACK_REF).set("lm-stack");
            op.get(Constants.FLAG).set("optional");
            DomainTestUtils.executeForResult(op, masterClient);

            op = new ModelNode();
            op.get(OP).set(READ_RESOURCE_OPERATION);
            op.get(OP_ADDR).set(jaspi.toModelNode());
            op.get(INCLUDE_ALIASES).set(false);
            op.get(RECURSIVE).set(true);

            ModelNode masterResource = DomainTestUtils.executeForResult(op, masterClient);
            ModelNode slaveResource = DomainTestUtils.executeForResult(op, slaveClient);

            ModelTestUtils.compare(masterResource, slaveResource, true);

        } finally {
            try {
                //The server will be in restart-required mode, so restart it to get rid of the changes
                PathAddress serverAddress = PathAddress.pathAddress(HOST, "slave").append(SERVER_CONFIG, "server-one");
                ModelNode op = Util.createOperation(RESTART, PathAddress.pathAddress(serverAddress));
                op.get(BLOCKING).set(true);
                Assert.assertEquals("STARTED", validateResponse(slaveClient.execute(op), true).asString());
            } finally {
                IoUtils.safeClose(slaveClient);
                IoUtils.safeClose(masterClient);
            }
        }
    }

    private Set<ModelNode> getAllChildren(ModelNode modules) {
        HashSet<ModelNode> set = new HashSet<ModelNode>();
        for (Property prop : modules.asPropertyList()) {
            set.add(prop.getValue());
        }
        return set;
    }

    private void cleanupKnownDifferencesInModelsForVersioningCheck(ModelNode masterModel, ModelNode slaveModel) {
        //First get rid of any undefined crap
        cleanUndefinedNodes(masterModel);
        cleanUndefinedNodes(slaveModel);
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
