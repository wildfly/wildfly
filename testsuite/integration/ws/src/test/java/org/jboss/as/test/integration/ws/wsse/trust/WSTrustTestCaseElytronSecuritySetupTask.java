/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class WSTrustTestCaseElytronSecuritySetupTask implements ServerSetupTask {
    public static final String HTTPS_SECURITY_REALM_NAME = "jbossws-https-realm";
    public static final String SECURITY_REALM_NAME = "ApplicationRealm";
    public static final String SECURITY_DOMAIN_NAME = "ApplicationDomain";
    public static final String HTTPS_LISTENER_NAME = "jbossws-https-listener";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();
        addSecurityRealm(operations);
        addHttpsListener(operations);
        addElytronSecurityDomain(operations);
        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();
        removeHttpsListener(operations);
        removeSecurityRealm(operations);
        removeElytronSecurityDomain(operations);
        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
        ServerReload.reloadIfRequired(managementClient);
    }
    /**
     * Add https listner like this:
     * <p/>
     * /**
     * <security-realm name="jbws-test-https-realm">
     * <server-identities>
     * <ssl>
     * <keystore path="/path/test.keystore" keystore-password="changeit" alias="tomcat"/>
     * </ssl>
     * </server-identities>
     * </security-realm>
     */
    private void addSecurityRealm(List<ModelNode> operations) throws Exception {
        final ModelNode addOp = createOpNode("core-service=management/security-realm=" + HTTPS_SECURITY_REALM_NAME, ADD);
        operations.add(addOp);
        final ModelNode addSslIdentityOp = createOpNode("core-service=management/security-realm=" + HTTPS_SECURITY_REALM_NAME + "/server-identity=ssl", ADD);
        addSslIdentityOp.get("keystore-path").set(WSTrustTestCaseElytronSecuritySetupTask.class.getResource("test.keystore").getPath());
        addSslIdentityOp.get("keystore-password").set("changeit");
        addSslIdentityOp.get("alias").set("tomcat");
        operations.add(addSslIdentityOp);
    }

    private void removeSecurityRealm(List<ModelNode> operations) throws Exception {
        final ModelNode removeOp = createOpNode("core-service=management/security-realm=" + HTTPS_SECURITY_REALM_NAME, REMOVE);
        operations.add(removeOp);
    }

    /**
     * Add https listner like this:
     * <p/>
     * <subsystem xmlns="urn:jboss:domain:undertow:3.0"> <server name="default-server"> <https-listener
     * name="jbws-test-https-listener" socket-binding="https" security-realm="jbws-test-https-realm"/> .... </server> ...
     * <subsystem>
     */
    private void addHttpsListener(List<ModelNode> operations) throws Exception {
        ModelNode addOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2", ADD);
        addOp.get(PORT).set("8444");
        operations.add(addOp);
        addOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, ADD);
        addOp.get(SOCKET_BINDING).set("https2");
        addOp.get(SECURITY_REALM).set(HTTPS_SECURITY_REALM_NAME);
        operations.add(addOp);
    }

    private void removeHttpsListener(List<ModelNode> operations) throws Exception {
        ModelNode removeOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2", REMOVE);
        operations.add(removeOp);
        removeOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, REMOVE);
        operations.add(removeOp);
    }

    private void addElytronSecurityDomain(List<ModelNode> operations) throws Exception {
        final ModelNode elytronHttpAuthOp = ModelUtil.createOpNode(
                "subsystem=elytron/http-authentication-factory=application-http-authentication", ADD);
        elytronHttpAuthOp.get("http-server-mechanism-factory").set("global");
        elytronHttpAuthOp.get("security-domain").set(SECURITY_DOMAIN_NAME);
        operations.add(elytronHttpAuthOp);

        final ModelNode addUndertowDomainOp = ModelUtil.createOpNode("subsystem=undertow/application-security-domain="
                + SECURITY_DOMAIN_NAME, ADD);
        addUndertowDomainOp.get("http-authentication-factory").set("application-http-authentication");
        operations.add(addUndertowDomainOp);

        final ModelNode addEJbDomainOp = ModelUtil.createOpNode("subsystem=ejb3/application-security-domain="
                + SECURITY_DOMAIN_NAME, ADD);
        addEJbDomainOp.get("security-domain").set(SECURITY_DOMAIN_NAME);
        operations.add(addEJbDomainOp);

    }

    private void removeElytronSecurityDomain(List<ModelNode> operations) throws Exception {
        final ModelNode removeUndertowDomainOp = ModelUtil.createOpNode(
                "subsystem=undertow/application-security-domain=" + SECURITY_DOMAIN_NAME, REMOVE);
        operations.add(removeUndertowDomainOp);
        final ModelNode removeEjbDomainOp = ModelUtil.createOpNode(
                "subsystem=ejb3/application-security-domain=" + SECURITY_DOMAIN_NAME, REMOVE);
        operations.add(removeEjbDomainOp);

    }


}
