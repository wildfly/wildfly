package org.jboss.as.test.integration.ws.wsse.trust;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;


public class WSTrustTestCaseSecuritySetupTask implements ServerSetupTask {

    public static final String SECURITY_REALM_NAME = "jbws-test-https-realm";
    public static final String SECURITY_DOMAIN_NAME = "JBossWS-trust-sts";
    public static final String HTTPS_LISTENER_NAME = "jbws-test-https-listener";

    private WSTrustSecurityDomainSetupTask securityDomainsSubtask = new WSTrustSecurityDomainSetupTask();

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();
        addSecurityRealm(operations);
        addHttpsListener(operations);
        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
        securityDomainsSubtask.setup(managementClient, containerId);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        securityDomainsSubtask.tearDown(managementClient, containerId);
        List<ModelNode> operations = new ArrayList<>();
        removeHttpsListener(operations);
        removeSecurityRealm(operations);
        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
        ServerReload.reloadIfRequired(managementClient);
    }


    /**
     * Add https listner like this:
     * <p/>
     * <subsystem xmlns="urn:jboss:domain:undertow:3.0">
     * <server name="default-server">
     * <https-listener name="jbws-test-https-listener" socket-binding="https" security-realm="jbws-test-https-realm"/>
     * ....
     * </server>
     * ...
     * </subsystem>
     */
    private void addHttpsListener(List<ModelNode> operations) throws Exception {
        ModelNode addOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2", ADD);
        addOp.get(PORT).set("8444");
        operations.add(addOp);
        addOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, ADD);
        addOp.get(SOCKET_BINDING).set("https2");
        addOp.get(SECURITY_REALM).set(SECURITY_REALM_NAME);
        operations.add(addOp);
    }

    private void removeHttpsListener(List<ModelNode> operations) throws Exception {
        ModelNode removeOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2", REMOVE);
        operations.add(removeOp);
        removeOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, REMOVE);
        operations.add(removeOp);
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
        final ModelNode addOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME, ADD);
        operations.add(addOp);
        final ModelNode addSslIdentityOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME + "/server-identity=ssl", ADD);
        addSslIdentityOp.get("keystore-path").set(WSTrustTestCaseSecuritySetupTask.class.getResource("test.keystore").getPath());
        addSslIdentityOp.get("keystore-password").set("changeit");
        addSslIdentityOp.get("alias").set("tomcat");
        operations.add(addSslIdentityOp);
    }

    private void removeSecurityRealm(List<ModelNode> operations) throws Exception {
        final ModelNode removeOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME, REMOVE);
        operations.add(removeOp);
    }


    class WSTrustSecurityDomainSetupTask extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {

            Map<String, String> loginOptions = new HashMap<>();
            loginOptions.put("usersProperties", WSTrustTestCaseSecuritySetupTask.class.getResource("WEB-INF/jbossws-users.properties").getPath());
            loginOptions.put("rolesProperties", WSTrustTestCaseSecuritySetupTask.class.getResource("WEB-INF/jbossws-roles.properties").getPath());
            loginOptions.put("unauthenticatedIdentity", "anonymous");

            SecurityModule loginModule = new SecurityModule.Builder()
                    .name("UsersRoles")
                    .flag("required")
                    .options(loginOptions)
                    .build();

            SecurityDomain securityDomain = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .cacheType("default")
                    .loginModules(loginModule)
                    .build();
            return new SecurityDomain[]{securityDomain};
        }
    }

}
