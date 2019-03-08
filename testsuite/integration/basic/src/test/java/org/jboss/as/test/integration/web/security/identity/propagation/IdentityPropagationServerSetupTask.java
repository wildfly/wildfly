/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.identity.propagation;

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Server setup task for test IdentityPropagationAuthenticationTestCase.
 * Configures Elytron Identity Propagation.
 */
public class IdentityPropagationServerSetupTask extends SnapshotRestoreSetupTask {

    protected String getSecurityDomainName() {
        return "auth-test";
    }

    protected String getUsersFile() {
        return new File(IdentityPropagationAuthenticationTestCase.class.getResource("users.properties").getFile()).getAbsolutePath();
    }

    protected String getGroupsFile() {
        return new File(IdentityPropagationAuthenticationTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath();
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

        // /subsystem=elytron/authentication-configuration=forwardit:add(security-domain=ApplicationDomain, sasl-mechanism-selector="#ALL")
        ModelNode addAuthenticationConfiguration = createOpNode("subsystem=elytron/authentication-configuration=forwardit", ADD);
        addAuthenticationConfiguration.get("authentication-name").set("theserver1");
        addAuthenticationConfiguration.get("security-domain").set("ApplicationDomain");
        addAuthenticationConfiguration.get("realm").set("ApplicationRealm");
        addAuthenticationConfiguration.get("forwarding-mode").set("authorization");
        //addAuthenticationConfiguration.get("sasl-mechanism-selector").set("#ALL");
        operations.add(addAuthenticationConfiguration);

        // /subsystem=elytron/authentication-context=forwardctx:add(match-rules=[{match-no-user=true, authentication-configuration=forwardit}])
        ModelNode addAuthenticationContext = createOpNode("subsystem=elytron/authentication-context=forwardctx", ADD);
        addAuthenticationContext.get("match-rules").get(0).get("match-no-user").set(true);
        addAuthenticationContext.get("match-rules").get(0).get("authentication-configuration").set("forwardit");
        operations.add(addAuthenticationContext);

        // /subsystem=elytron/simple-permission-mapper=default-permission-mapper:
        // write-attribute(name=permission-mappings[1], value={principals=[anonymous], permissions=[
        // {class-name="org.wildfly.security.auth.permission.RunAsPrincipalPermission",target-name="*"},
        // {class-name="org.wildfly.security.auth.permission.LoginPermission"}
        // {class-name=org.wildfly.extension.batch.jberet.deployment.BatchPermission, module=org.wildfly.extension.batch.jberet, target-name=*},
        // {class-name=org.wildfly.transaction.client.RemoteTransactionPermission, module=org.wildfly.transaction.client},
        // {class-name=org.jboss.ejb.client.RemoteEJBPermission, module=org.jboss.ejb-client}]})
        ModelNode setPermissionMapping1 = createOpNode("subsystem=elytron/simple-permission-mapper=default-permission-mapper", WRITE_ATTRIBUTE_OPERATION);
        setPermissionMapping1.get(ClientConstants.NAME).set("permission-mappings[1]");
        setPermissionMapping1.get(ClientConstants.VALUE).get("principals").get(0).set("theserver1");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(0).get("class-name").set("org.wildfly.security.auth.permission.RunAsPrincipalPermission");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(0).get("target-name").set("*");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(1).get("class-name").set("org.wildfly.security.auth.permission.LoginPermission");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(2).get("class-name").set("org.wildfly.extension.batch.jberet.deployment.BatchPermission");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(2).get("module").set("org.wildfly.extension.batch.jberet");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(2).get("target-name").set("*");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(3).get("class-name").set("org.wildfly.transaction.client.RemoteTransactionPermission");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(3).get("module").set("org.wildfly.transaction.client");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(4).get("class-name").set("org.jboss.ejb.client.RemoteEJBPermission");
        setPermissionMapping1.get(ClientConstants.VALUE).get("permissions").get(4).get("module").set("org.jboss.ejb-client");
        operations.add(setPermissionMapping1);

        // /subsystem=elytron/simple-permission-mapper=default-permission-mapper:
        // write-attribute(name=permission-mappings[2], value={match-all=true, permissions=[
        // {class-name=org.wildfly.security.auth.permission.LoginPermission},
        // {class-name=org.wildfly.extension.batch.jberet.deployment.BatchPermission, module=org.wildfly.extension.batch.jberet, target-name=*},
        // {class-name=org.wildfly.transaction.client.RemoteTransactionPermission,module=org.wildfly.transaction.client},
        // {class-name=org.jboss.ejb.client.RemoteEJBPermission, module=org.jboss.ejb-client}]})
        ModelNode setPermissionMapping2 = createOpNode("subsystem=elytron/simple-permission-mapper=default-permission-mapper", WRITE_ATTRIBUTE_OPERATION);
        setPermissionMapping2.get(ClientConstants.NAME).set("permission-mappings[2]");
        setPermissionMapping2.get(ClientConstants.VALUE).get("match-all").set(true);
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(0).get("class-name").set("org.wildfly.security.auth.permission.LoginPermission");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(1).get("class-name").set("org.wildfly.extension.batch.jberet.deployment.BatchPermission");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(1).get("module").set("org.wildfly.extension.batch.jberet");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(1).get("target-name").set("*");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(2).get("class-name").set("org.wildfly.transaction.client.RemoteTransactionPermission");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(2).get("module").set("org.wildfly.transaction.client");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(3).get("class-name").set("org.jboss.ejb.client.RemoteEJBPermission");
        setPermissionMapping2.get(ClientConstants.VALUE).get("permissions").get(3).get("module").set("org.jboss.ejb-client");
        operations.add(setPermissionMapping2);

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
