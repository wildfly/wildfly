package org.jboss.as.test.integration.ws.wsse.trust;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.dmr.ModelNode;

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

        addSecurityRealm(managementClient);
        addHttpsListener(managementClient);
        securityDomainsSubtask.setup(managementClient, containerId);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

        securityDomainsSubtask.tearDown(managementClient, containerId);
        removeHttpsListener(managementClient);
        removeSecurityRealm(managementClient);
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
    private void addHttpsListener(ManagementClient managementClient) throws Exception {


        ModelNode addOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2", ADD);
        addOp.get(PORT).set("8444");
        addOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        addOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(addOp, managementClient.getControllerClient());
        addOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, ADD);
        addOp.get(SOCKET_BINDING).set("https2");
        addOp.get(SECURITY_REALM).set(SECURITY_REALM_NAME);
        addOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        addOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(addOp, managementClient.getControllerClient());
    }

    private void removeHttpsListener(ManagementClient managementClient) throws Exception {
        ModelNode removeOp = createOpNode("socket-binding-group=standard-sockets/socket-binding=https2" + HTTPS_LISTENER_NAME, REMOVE);
        removeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        removeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(removeOp, managementClient.getControllerClient());
        removeOp = createOpNode("subsystem=undertow/server=default-server/https-listener=" + HTTPS_LISTENER_NAME, REMOVE);
        removeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        removeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(removeOp, managementClient.getControllerClient());
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
    private void addSecurityRealm(ManagementClient managementClient) throws Exception {
        final ModelNode addOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME, ADD);
        addOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        addOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(addOp, managementClient.getControllerClient());

        final ModelNode addSslIdentityOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME + "/server-identity=ssl", ADD);
        addSslIdentityOp.get("keystore-path").set(WSTrustTestCaseSecuritySetupTask.class.getResource("test.keystore").getPath());
        addSslIdentityOp.get("keystore-password").set("changeit");
        addSslIdentityOp.get("alias").set("tomcat");
        addSslIdentityOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        addSslIdentityOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(addSslIdentityOp, managementClient.getControllerClient());
    }

    private void removeSecurityRealm(ManagementClient managementClient) throws Exception {
        final ModelNode removeOp = createOpNode("core-service=management/security-realm=" + SECURITY_REALM_NAME, REMOVE);
        removeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        removeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(removeOp, managementClient.getControllerClient());
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
