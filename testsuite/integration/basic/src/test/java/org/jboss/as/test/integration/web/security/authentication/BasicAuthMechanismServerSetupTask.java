/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.web.security.authentication;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Server setup task for test BasicAuthenticationMechanismPicketboxRemovedTestCase.
 * Enables elytron and removes security subsystem.
 */
public class BasicAuthMechanismServerSetupTask extends SnapshotRestoreSetupTask {

    protected String getSecurityDomainName() {
        return "auth-test";
    }

    protected String getUsersFile() {
        return new File(BasicAuthMechanismServerSetupTask.class.getResource("users.properties").getFile()).getAbsolutePath();
    }

    protected String getGroupsFile() {
        return new File(BasicAuthMechanismServerSetupTask.class.getResource("roles.properties").getFile()).getAbsolutePath();
    }

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();

        // /subsystem=elytron/properties-realm=auth-test-ejb3-UsersRoles:add(users-properties={path=users.properties, plain-text=true},groups-properties={path=roles.properties})
        // /subsystem=elytron/security-domain=auth-test:add(default-realm=auth-test-ejb3-UsersRoles, realms=[{realm=auth-test-ejb3-UsersRoles}])
        ElytronDomainSetup elytronDomainSetup = new ElytronDomainSetup(getUsersFile(), getGroupsFile(), getSecurityDomainName());
        elytronDomainSetup.setup(managementClient, containerId);

        // /subsystem=elytron/http-authentication-factory=auth-test:add(http-server-mechanism-factory=global,security-domain=auth-test,mechanism-configurations=[{mechanism-name=BASIC}])
        // /subsystem=undertow/application-security-domain=auth-test:add(http-authentication-factory=auth-test)
        ServletElytronDomainSetup servletElytronDomainSetup = new ServletElytronDomainSetup(getSecurityDomainName());
        servletElytronDomainSetup.setup(managementClient, containerId);

        // /subsystem=elytron/sasl-authentication-factory=auth-test:add(sasl-server-factory=configured,security-domain=auth-test,mechanism-configurations=[{mechanism-name=BASIC}])
        ModelNode addSaslAuthentication = createOpNode("subsystem=elytron/sasl-authentication-factory=" + getSecurityDomainName(), ADD);
        addSaslAuthentication.get("sasl-server-factory").set("configured");
        addSaslAuthentication.get("security-domain").set(getSecurityDomainName());
        addSaslAuthentication.get("mechanism-configurations").get(0).get("mechanism-name").set("PLAIN");
        operations.add(addSaslAuthentication);
        // /subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory, value=auth-test)
        ModelNode updateRemotingConnector = createOpNode("subsystem=remoting/http-connector=http-remoting-connector", WRITE_ATTRIBUTE_OPERATION);
        updateRemotingConnector.get(ClientConstants.NAME).set("sasl-authentication-factory");
        updateRemotingConnector.get(ClientConstants.VALUE).set(getSecurityDomainName());
        operations.add(updateRemotingConnector);
        // subsystem=remoting/http-connector=http-remoting-connector:undefine-attribute(name=security-realm)
        ModelNode undefineAttrOp2 = createOpNode("subsystem=remoting/http-connector=http-remoting-connector", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrOp2.get(ClientConstants.NAME).set("security-realm");
        operations.add(undefineAttrOp2);

        // /subsystem=ejb3/application-security-domain=auth-test:add(security-domain=auth-test)
        ModelNode addEjbDomain = createOpNode("subsystem=ejb3/application-security-domain=" + getSecurityDomainName(), ADD);
        addEjbDomain.get("security-domain").set(getSecurityDomainName());
        operations.add(addEjbDomain);
        // /subsystem=ejb3:write-attribute(name=default-missing-method-permissions-deny-access, value=false)
        ModelNode updateDefaultMissingMethod = createOpNode("subsystem=ejb3", WRITE_ATTRIBUTE_OPERATION);
        updateDefaultMissingMethod.get(ClientConstants.NAME).set("default-missing-method-permissions-deny-access");
        updateDefaultMissingMethod.get(ClientConstants.VALUE).set(false);
        operations.add(updateDefaultMissingMethod);

        // core-service=management/management-interface=http-interface:write-attribute(name=http-upgrade,value={enabled=true, sasl-authentication-factory=management-sasl-authentication})
        ModelNode writeAttrOp4 = createOpNode("core-service=management/management-interface=http-interface", WRITE_ATTRIBUTE_OPERATION);
        writeAttrOp4.get(ClientConstants.NAME).set("http-upgrade");
        writeAttrOp4.get(ClientConstants.VALUE).add("enabled", true);
        writeAttrOp4.get(ClientConstants.VALUE).add("sasl-authentication-factory", getSecurityDomainName());
        operations.add(writeAttrOp4);
        // core-service=management/management-interface=http-interface:write-attribute(name=http-authentication-factory,value=management-http-authentication)
        ModelNode writeAttrOp5 = createOpNode("core-service=management/management-interface=http-interface", WRITE_ATTRIBUTE_OPERATION);
        writeAttrOp5.get(ClientConstants.NAME).set("http-authentication-factory");
        writeAttrOp5.get(ClientConstants.VALUE).set(getSecurityDomainName());
        operations.add(writeAttrOp5);
        // core-service=management/management-interface=http-interface:undefine-attribute(name=security-realm)
        ModelNode undefineAttrOp3 = createOpNode("core-service=management/management-interface=http-interface", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrOp3.get(ClientConstants.NAME).set("security-realm");
        operations.add(undefineAttrOp3);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        ModelNode removeSecurityOp = new ModelNode();
        removeSecurityOp.get(OP).set(REMOVE);
        removeSecurityOp.get(OP_ADDR).add(SUBSYSTEM, "security");
        CoreUtils.applyUpdate(removeSecurityOp, managementClient.getControllerClient());
    }
}
