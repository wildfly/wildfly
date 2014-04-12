/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBAccessException;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.ejb.container.interceptor.security.CurrentUserCredential;
import org.jboss.as.test.integration.ejb.container.interceptor.security.GuestDelegationLoginModule;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor.Registration;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.security.ClientLoginModule;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.auth.callback.UsernamePasswordHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase based on ejb-security-interceptors quickstart application. It tests security context propagation for EJBs.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ SwitchIdentityTestCase.SecurityDomainsSetup.class, //
        SwitchIdentityTestCase.SecurityRealmsSetup.class, //
        SwitchIdentityTestCase.RemotingSetup.class })
@RunAsClient
public class SwitchIdentityTestCase {

    private static final String EJB_OUTBOUND_SOCKET_BINDING = "ejb-outbound";

    private static Logger LOGGER = Logger.getLogger(SwitchIdentityTestCase.class);

    private static final String EJB_OUTBOUND_REALM = "ejb-outbound-realm";
    private static final String SECURITY_DOMAIN_NAME = "switch-identity-test";

    private static final PathAddress ADDR_SOCKET_BINDING = PathAddress.pathAddress()
            .append(SOCKET_BINDING_GROUP, "standard-sockets")
            .append(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, EJB_OUTBOUND_SOCKET_BINDING);
    private static final PathAddress ADDR_REMOTING_CONNECTOR = PathAddress.pathAddress().append(SUBSYSTEM, "remoting")
            .append("remote-outbound-connection", "ejb-outbound-connection");

    /**
     * The login {@link Configuration} which always returns a single {@link AppConfigurationEntry} with a
     * {@link ClientLoginModule}.
     */
    private static final Configuration CLIENT_LOGIN_CONFIG = new Configuration() {

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            final Map<String, String> options = new HashMap<String, String>();
            options.put("multi-threaded", "true");
            options.put("restore-login-identity", "true");

            AppConfigurationEntry clmEntry = new AppConfigurationEntry(ClientLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);

            return new AppConfigurationEntry[] { clmEntry };
        }
    };

    @ArquillianResource
    private ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates a deployment application for this test.
     *
     * @return
     * @throws IOException
     */
    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EJBUtil.APPLICATION_NAME + ".jar");
        jar.addClasses(GuestDelegationLoginModule.class, EJBUtil.class, Manage.class, BridgeBean.class, TargetBean.class,
                CurrentUserCredential.class, ServerSecurityInterceptor.class, ClientSecurityInterceptor.class);
        jar.addAsManifestResource(SwitchIdentityTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(new StringAsset(ClientSecurityInterceptor.class.getName()),
                "services/org.jboss.ejb.client.EJBClientInterceptor");
        jar.addAsManifestResource(ClientSecurityInterceptor.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(ClientSecurityInterceptor.class.getPackage(), "permissions.xml", "permissions.xml");
        jar.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.as.security-api", "org.jboss.as.core-security-api"), "jboss-deployment-structure.xml");
        final Properties props = EJBUtil.createEjbClientConfiguration(StringUtils.strip(
                TestSuiteEnvironment.getServerAddress(), "[]"));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);
        jar.addAsResource(new ByteArrayAsset(baos.toByteArray()), "jboss-ejb-client.properties");
        return jar;
    }

    /**
     * Test identity propagation using SecurityContextAssociation API from the client.
     *
     * @throws Exception
     */
    @Test
    public void testSecurityContextAssociation() throws Exception {
        callUsingSecurityContextAssociation("guest", false, false);
        callUsingSecurityContextAssociation("user1", true, false);
        callUsingSecurityContextAssociation("user2", false, true);
    }

    /**
     * Test identity propagation using LoginContext API from the client.
     *
     * @throws Exception
     */
    @Test
    public void testClientLoginModule() throws Exception {
        callUsingClientLoginModul("guest", false, false);
        callUsingClientLoginModul("user1", true, false);
        callUsingClientLoginModul("user2", false, true);
    }

    // Private methods -------------------------------------------------------

    /**
     * Perform the tests using the ClientLoginModule and LoginContext API to set the desired Principal.
     */
    private void callUsingClientLoginModul(String userName, boolean hasRole1, boolean hasRole2) throws Exception {
        LoginContext loginContext = null;
        try {
            loginContext = new LoginContext("foo", new Subject(), new UsernamePasswordHandler(userName, new char[0]),
                    CLIENT_LOGIN_CONFIG);
            loginContext.login();

            // register the client side interceptor
            final Registration clientInterceptorHandler = EJBClientContext.requireCurrent().registerInterceptor(112567,
                    new ClientSecurityInterceptor());

            final Manage targetBean = EJBUtil.lookupEJB(TargetBean.class, Manage.class);
            final Manage bridgeBean = EJBUtil.lookupEJB(BridgeBean.class, Manage.class);

            //test direct access
            testMethodAccess(targetBean, ManageMethodEnum.ALLROLES, true);
            testMethodAccess(targetBean, ManageMethodEnum.ROLE1, hasRole1);
            testMethodAccess(targetBean, ManageMethodEnum.ROLE2, hasRole2);

            //test security context propagation
            testMethodAccess(bridgeBean, ManageMethodEnum.ALLROLES, true);
            testMethodAccess(bridgeBean, ManageMethodEnum.ROLE1, hasRole1);
            testMethodAccess(bridgeBean, ManageMethodEnum.ROLE2, hasRole2);

            clientInterceptorHandler.remove();
        } finally {
            if (loginContext != null) {
                loginContext.logout();
            }
        }
    }

    /**
     * Perform the tests using the SecurityContextAssociation API to set the desired Principal.
     */
    private void callUsingSecurityContextAssociation(String userName, boolean hasRole1, boolean hasRole2) throws Exception {
        try {
            final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient));
            EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(ejbClientConfiguration);
            final ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc);
            EJBClientContext.setSelector(selector);
            // register the client side interceptor
            final Registration clientInterceptorHandler = EJBClientContext.requireCurrent().registerInterceptor(112567,
                    new ClientSecurityInterceptor());
            SecurityContextAssociation.setPrincipal(new SimplePrincipal(userName));

            final Manage targetBean = EJBUtil.lookupEJB(TargetBean.class, Manage.class);
            final Manage bridgeBean = EJBUtil.lookupEJB(BridgeBean.class, Manage.class);

            //test direct access
            testMethodAccess(targetBean, ManageMethodEnum.ALLROLES, true);
            testMethodAccess(targetBean, ManageMethodEnum.ROLE1, hasRole1);
            testMethodAccess(targetBean, ManageMethodEnum.ROLE2, hasRole2);

            //test security context propagation
            testMethodAccess(bridgeBean, ManageMethodEnum.ALLROLES, true);
            testMethodAccess(bridgeBean, ManageMethodEnum.ROLE1, hasRole1);
            testMethodAccess(bridgeBean, ManageMethodEnum.ROLE2, hasRole2);

            clientInterceptorHandler.remove();
        } finally {
            SecurityContextAssociation.clearSecurityContext();
        }
    }

    /**
     * Tests access to a single method of a {@link Manage} EJB implementation.
     *
     * @param bean EJB instance
     * @param method method type
     * @param hasAccess expected value
     */
    private void testMethodAccess(Manage bean, ManageMethodEnum method, boolean hasAccess) {
        try {
            final String result;
            switch (method) {
                case ROLE1:
                    result = bean.role1();
                    break;
                case ROLE2:
                    result = bean.role2();
                    break;
                case ALLROLES:
                    result = bean.allRoles();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown bean method type.");
            }
            assertEquals(Manage.RESULT, result);
            if (!hasAccess) {
                fail("Acess should be denied.");
            }
        } catch (EJBAccessException e) {
            if (hasAccess) {
                fail("Access should be allowed.");
            }
        }

    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * <pre>
         * &lt;security-domain name=&quot;switch-identity-test&quot; cache-type=&quot;default&quot;&gt;
         *     &lt;authentication&gt;
         *         &lt;login-module code=&quot;{@link GuestDelegationLoginModule}&quot; flag=&quot;optional&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *         &lt;login-module code=&quot;Remoting&quot; flag=&quot;optional&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *         &lt;login-module code=&quot;RealmDirect&quot; flag=&quot;required&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *     &lt;/authentication&gt;
         * &lt;/security-domain&gt;
         * </pre>
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().flag("optional").putOption(
                    "password-stacking", "useFirstPass");
            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .loginModules(loginModuleBuilder.name(GuestDelegationLoginModule.class.getName()).build(),
                            loginModuleBuilder.name("Remoting").build(), //
                            loginModuleBuilder.name("RealmDirect").build()) //
                    .build();
            return new SecurityDomain[] { sd };
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates security realms for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {

        /**
         * Returns SecurityRealms configuration for this testcase.
         *
         * <pre>
         * &lt;security-realm name=&quot;ejb-outbound-realm&quot;&gt;
         *   &lt;server-identities&gt;
         *      &lt;secret value=&quot;xxx&quot;/&gt;
         *   &lt;/server-identities&gt;
         * &lt;/security-realm&gt;
         * </pre>
         *
         */
        @Override
        protected SecurityRealm[] getSecurityRealms() {
            final ServerIdentity serverIdentity = new ServerIdentity.Builder().secretPlain(EJBUtil.CONNECTION_PASSWORD).build();
            final SecurityRealm realm = new SecurityRealm.Builder().name(EJB_OUTBOUND_REALM).serverIdentity(serverIdentity)
                    .build();
            return new SecurityRealm[] { realm };
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates remoting mappings for this test case.
     *
     * @author Josef Cacek
     */
    static class RemotingSetup implements ServerSetupTask {

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final List<ModelNode> updates = new LinkedList<ModelNode>();
            LOGGER.info("Adding socket binding");
            // /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=ejb-outbound:add(host=localhost,port=8080) {allow-resource-service-restart=true}
            ModelNode socketBindingModelNode = Util.createAddOperation(ADDR_SOCKET_BINDING);
            socketBindingModelNode.get(HOST).set(Utils.getHost(managementClient));
            socketBindingModelNode.get(PORT).set(8080);
            socketBindingModelNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(socketBindingModelNode);

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=remoting/remote-outbound-connection=ejb-outbound-connection:add(outbound-socket-binding-ref=ejb-outbound,username=ConnectionUser,security-realm=ejb-outbound-realm) {allow-resource-service-restart=true}
            final ModelNode remotingConnectorModelNode = Util.createAddOperation(ADDR_REMOTING_CONNECTOR);
            remotingConnectorModelNode.get("outbound-socket-binding-ref").set(EJB_OUTBOUND_SOCKET_BINDING);
            remotingConnectorModelNode.get("username").set(EJBUtil.CONNECTION_PASSWORD);
            remotingConnectorModelNode.get("security-realm").set(EJB_OUTBOUND_REALM);
            remotingConnectorModelNode.get("protocol").set("http-remoting");
            remotingConnectorModelNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(remotingConnectorModelNode);

            // /subsystem=remoting/remote-outbound-connection=ejb-outbound-connection/property=SSL_ENABLED:add(value=false)
            final ModelNode sslPropertyModelNode = Util.createAddOperation(ADDR_REMOTING_CONNECTOR.append("property",
                    "SSL_ENABLED"));
            sslPropertyModelNode.get(VALUE).set(false);
            sslPropertyModelNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(sslPropertyModelNode);

            updates.add(compositeOp);
            Utils.applyUpdates(updates, managementClient.getControllerClient());

        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            // /subsystem=remoting/remote-outbound-connection=ejb-outbound-connection:remove()
            ModelNode op = Util.createRemoveOperation(ADDR_REMOTING_CONNECTOR);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=ejb-outbound:remove()
            op = Util.createRemoveOperation(ADDR_SOCKET_BINDING);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            Utils.applyUpdates(updates, managementClient.getControllerClient());
        }
    }

    /**
     * An Enum, which holds expected method types in {@link Manage} interface.
     *
     * @author Josef Cacek
     */
    private enum ManageMethodEnum {
        ROLE1, ROLE2, ALLROLES;
    }

}
