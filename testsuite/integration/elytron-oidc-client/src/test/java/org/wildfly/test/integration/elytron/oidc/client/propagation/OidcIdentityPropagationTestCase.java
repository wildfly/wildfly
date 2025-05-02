/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.propagation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assume.assumeTrue;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.KEYCLOAK_CONTAINER;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.loginToApp;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;
import org.wildfly.test.integration.elytron.oidc.client.propagation.base.WhoAmIBean;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.AnotherEntryBeanLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.AnotherEntryLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.AnotherSimpleServletLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.ComplexServletLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.EntryBeanLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.EntryLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.ManagementBeanLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.ServletOnlyLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.SimpleServletLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.WhoAmIBeanLocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.local.WhoAmILocal;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.ComplexServletRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.EntryBeanRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.EntryRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.ManagementBeanRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.ServletOnlyRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.SimpleServletRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.WhoAmIBeanRemote;
import org.wildfly.test.integration.elytron.oidc.client.propagation.remote.WhoAmIRemote;
import org.wildfly.test.security.common.elytron.EjbElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;

import io.restassured.RestAssured;

/**
 * Test class to hold the identity propagation scenarios involving a virtual security domain with OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcIdentityPropagationTestCase.EJBDomainSetupOverride.class, OidcIdentityPropagationTestCase.AnotherEJBDomainSetupOverride.class, OidcIdentityPropagationTestCase.PropagationSetup.class, OidcIdentityPropagationTestCase.KeycloakAndSubsystemSetup.class })
public class OidcIdentityPropagationTestCase {

    private static final String TEST_REALM = "WildFly";
    private static final int CLIENT_PORT = TestSuiteEnvironment.getHttpPort();
    private static final String CLIENT_HOST_NAME = TestSuiteEnvironment.getHttpAddress();
    private static final String CLIENT_SECRET = "secret";
    private static final String OIDC_PROVIDER_URL = "oidc.provider.url";
    private static final String SINGLE_DEPLOYMENT_LOCAL = "single-deployment-local";
    private static final String ANOTHER_SINGLE_DEPLOYMENT_LOCAL = "another-single-deployment-local";
    private static final String NO_OUTFLOW_CONFIG = "no-outflow-config";
    private static final String OUTFLOW_ANONYMOUS_CONFIG = "outflow-anonymous-config";
    private static final String EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL = "ear-servlet-ejb-deployment-local";
    private static final String EAR_DEPLOYMENT_WITH_SERVLET_REMOTE = "ear-servlet-remote";
    private static final String EAR_DEPLOYMENT_WITH_SERVLET_LOCAL = "ear-servlet-local";
    private static final String SINGLE_DEPLOYMENT_REMOTE = "single-deployment-remote";
    private static final String EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE = "ear-servlet-ejb-deployment-remote";
    private static final String EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN = "ear-servlet-ejb-deployment-remote-same-domain";
    private static final String EAR_DEPLOYMENT_WITH_EJB_LOCAL = "ear-ejb-deployment-local";
    private static final String EAR_DEPLOYMENT_WITH_EJB_REMOTE = "ear-ejb-deployment-remote";
    private static final String EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN = "ear-ejb-deployment-remote-same-domain";
    private static final String EJB_SECURITY_DOMAIN_NAME = "ejb-domain";
    private static final String ANOTHER_EJB_SECURITY_DOMAIN_NAME = "another-ejb-domain";
    private static final String SECURE_DEPLOYMENT_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/secure-deployment=";
    private static final String PROVIDER_ADDRESS = "subsystem=" + ElytronOidcExtension.SUBSYSTEM_NAME + "/provider=";
    private static final String KEYCLOAK_PROVIDER = "keycloak";

    private static Map<String, KeycloakConfiguration.ClientAppType> CLIENT_IDS;
    static {
        CLIENT_IDS = new HashMap<>();
        CLIENT_IDS.put(SINGLE_DEPLOYMENT_LOCAL + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(ANOTHER_SINGLE_DEPLOYMENT_LOCAL + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(NO_OUTFLOW_CONFIG + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(OUTFLOW_ANONYMOUS_CONFIG + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(EAR_DEPLOYMENT_WITH_SERVLET_REMOTE + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(EAR_DEPLOYMENT_WITH_SERVLET_LOCAL + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(SINGLE_DEPLOYMENT_REMOTE + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        CLIENT_IDS.put(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN + "-web", KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
    }

    private static ArrayList<String> APP_NAMES = new ArrayList<>(Arrays.asList(SINGLE_DEPLOYMENT_LOCAL, ANOTHER_SINGLE_DEPLOYMENT_LOCAL, OUTFLOW_ANONYMOUS_CONFIG, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL,
            EAR_DEPLOYMENT_WITH_SERVLET_REMOTE, EAR_DEPLOYMENT_WITH_SERVLET_LOCAL, SINGLE_DEPLOYMENT_REMOTE, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN));

    @ArquillianResource
    private URL url;

    @ArquillianResource
    protected static Deployer deployer;

    @Deployment(name= EAR_DEPLOYMENT_WITH_EJB_LOCAL, order = 1)
    public static Archive<?> ejbDeploymentLocal() {
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_EJB_LOCAL + "-ejb.jar")
                .addClass(WhoAmIBeanLocal.class).addClass(EntryBeanLocal.class)
                .addClass(WhoAmILocal.class).addClass(EntryLocal.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_EJB_LOCAL + ".ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_EJB_REMOTE, order = 2)
    public static Archive<?> ejbDeploymentRemote() {
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_EJB_REMOTE + "-ejb.jar")
                .addClass(WhoAmIBeanRemote.class).addClass(EntryBeanRemote.class)
                .addClass(WhoAmIRemote.class).addClass(EntryRemote.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_EJB_REMOTE + ".ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= SINGLE_DEPLOYMENT_LOCAL, order = 3)
    public static Archive<?> singleDeploymentLocal() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SINGLE_DEPLOYMENT_LOCAL + "-web.war")
                .addClasses(SimpleServletLocal.class, OidcIdentityPropagationTestCase.class)
                .addClass(Util.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLE_DEPLOYMENT_LOCAL + "-ejb.jar")
                .addClass(WhoAmIBeanLocal.class).addClass(EntryBeanLocal.class)
                .addClass(WhoAmILocal.class).addClass(EntryLocal.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SINGLE_DEPLOYMENT_LOCAL + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);

        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL, order = 4)
    public static Archive<?> servletAndEJBDeploymentLocal() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL + "-web.war")
                .addClasses(ComplexServletLocal.class, OidcIdentityPropagationTestCase.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        PropagationSetup.class, AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL + "-ejb.jar")
                .addClass(ManagementBeanLocal.class)
                .addClass(Util.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_LOCAL + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_LOCAL + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_SERVLET_REMOTE, order = 5)
    public static Archive<?> servletOnlyRemote() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_REMOTE + "-web.war")
                .addClasses(ServletOnlyRemote.class, OidcIdentityPropagationTestCase.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_REMOTE + "-ejb.jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_REMOTE + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_REMOTE + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_REMOTE + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_SERVLET_LOCAL, order = 6)
    public static Archive<?> servletOnlyLocal() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_LOCAL + "-web.war")
                .addClasses(ServletOnlyLocal.class, OidcIdentityPropagationTestCase.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_LOCAL + "-ejb.jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_LOCAL + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_LOCAL + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_LOCAL + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= SINGLE_DEPLOYMENT_REMOTE, order = 7)
    public static Archive<?> singleDeploymentRemote() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SINGLE_DEPLOYMENT_REMOTE + "-web.war")
                .addClasses(SimpleServletRemote.class, OidcIdentityPropagationTestCase.class)
                .addClass(Util.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLE_DEPLOYMENT_REMOTE + "-ejb.jar")
                .addClass(WhoAmIBeanRemote.class).addClass(EntryBeanRemote.class)
                .addClass(WhoAmIRemote.class).addClass(EntryRemote.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SINGLE_DEPLOYMENT_REMOTE + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE, order = 8)
    public static Archive<?> servletAndEJBDeploymentRemote() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE + "-web.war")
                .addClasses(ComplexServletRemote.class, OidcIdentityPropagationTestCase.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        PropagationSetup.class, AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE + "-ejb.jar")
                .addClass(ManagementBeanRemote.class)
                .addClass(Util.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_REMOTE + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_REMOTE + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= ANOTHER_SINGLE_DEPLOYMENT_LOCAL, order = 9)
    public static Archive<?> anotherSingleDeploymentLocal() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ANOTHER_SINGLE_DEPLOYMENT_LOCAL + "-web.war")
                .addClasses(AnotherSimpleServletLocal.class, OidcIdentityPropagationTestCase.class)
                .addClass(Util.class)
                .addClasses(EJBDomainSetupOverride.class, AnotherEJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ANOTHER_SINGLE_DEPLOYMENT_LOCAL + "-ejb.jar")
                .addClass(AnotherEntryBeanLocal.class)
                .addClass(AnotherEntryLocal.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ANOTHER_SINGLE_DEPLOYMENT_LOCAL + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);

        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= NO_OUTFLOW_CONFIG, order = 10)
    public static Archive<?> noOutflowConfig() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, NO_OUTFLOW_CONFIG + "-web.war")
                .addClasses(SimpleServletLocal.class, OidcIdentityPropagationTestCase.class)
                .addClass(Util.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, NO_OUTFLOW_CONFIG + "-ejb.jar")
                .addClass(WhoAmIBeanLocal.class).addClass(EntryBeanLocal.class)
                .addClass(WhoAmILocal.class).addClass(EntryLocal.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, NO_OUTFLOW_CONFIG + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);

        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= OUTFLOW_ANONYMOUS_CONFIG, order = 11)
    public static Archive<?> outflowAnonymousConfig() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, OUTFLOW_ANONYMOUS_CONFIG + "-web.war")
                .addClasses(SimpleServletLocal.class, OidcIdentityPropagationTestCase.class)
                .addClass(Util.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, OUTFLOW_ANONYMOUS_CONFIG + "-ejb.jar")
                .addClass(WhoAmIBeanLocal.class).addClass(EntryBeanLocal.class)
                .addClass(WhoAmILocal.class).addClass(EntryLocal.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, OUTFLOW_ANONYMOUS_CONFIG + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);

        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN, order = 12)
    public static Archive<?> ejbDeploymentRemoteSameDomain() {
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN + "-ejb.jar")
                .addClass(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.WhoAmIBeanRemote.class)
                .addClass(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.EntryBeanRemote.class)
                .addClass(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.WhoAmIRemote.class)
                .addClass(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.EntryRemote.class)
                .addClass(WhoAmIBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN + ".ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN, order = 13)
    public static Archive<?> servletAndEJBDeploymentRemoteSameDomain() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = OidcIdentityPropagationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN + "-web.war")
                .addClasses(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.ComplexServletRemote.class, OidcIdentityPropagationTestCase.class)
                .addClasses(EJBDomainSetupOverride.class, PropagationSetup.class,
                        PropagationSetup.class, AbstractMgmtTestBase.class, EjbElytronDomainSetup.class);
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN + "-ejb.jar")
                .addClass(org.wildfly.test.integration.elytron.oidc.client.propagation.annotation.ManagementBeanRemote.class)
                .addClass(Util.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_REMOTE_SAME_DOMAIN + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(createPermissionsXmlAsset(
                        // Util#switchIdentity calls SecurityDomain#getCurrent and SecurityDomain#authenticate
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                ),
                "permissions.xml");
        return ear;
    }

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
    }

    /**
     * Servlet -> EJB propagation scenarios involving different security domains within a single deployment.
     */

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a local EJB within a JAR
     *
     * The servlet attempts to invoke the local EJB.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT_LOCAL)
    public void testServletToEjbSingleDeploymentLocal() throws Exception {
        testServletToEjbInvocation();
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a remote EJB within a JAR
     *
     * The servlet attempts to invoke the remote EJB.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT_REMOTE)
    public void testServletToEjbSingleDeploymentRemote() throws Exception {
        testServletToEjbInvocation();
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - an EJB within a JAR
     *
     * The wrong password is used to access the servlet.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT_REMOTE)
    public void testWrongPasswordSingleDeployment() throws Exception {
        testWrongPassword();
    }

    private void testServletToEjbInvocation() throws Exception {
        String expectedMessage = "alice,true"; // alice should have Managers role
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage,
                true, url.toURI().resolve("whoAmI"));
    }

    private void testWrongPassword() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password", true, url.toURI().resolve("whoAmI"));
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a local EJB within a JAR
     *
     * The servlet attempts to invoke the local EJB.
     * The identity being used to invoke the servlet doesn't exist in the security
     * domain that's being used to secure the EJB.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT_LOCAL)
    public void testServletToEjbSingleDeploymentOutflowNotPossibleNonExistentIdentity() throws Exception {
        String expectedMessage = "anonymous,false"; // charlie won't be outflowed, the target security domain's current identity will be used instead
        loginToApp(KeycloakConfiguration.CHARLIE, KeycloakConfiguration.CHARLIE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage, true, url.toURI().resolve("whoAmI"));
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a local EJB within a JAR
     *
     * The servlet attempts to invoke the local EJB.
     * The security domain that's being used to secure the EJB hasn't been configured to
     * trust the virtual security domain.
     */
    @Test
    @OperateOnDeployment(ANOTHER_SINGLE_DEPLOYMENT_LOCAL)
    public void testServletToEjbSingleDeploymentOutflowNotPossibleTrustNotConfigured() throws Exception {
        String expectedMessage = "anonymous,false"; // charlie won't be outflowed, the target security domain's current identity will be used instead
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage, true, url.toURI().resolve("anotherWhoAmI"));
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a local EJB within a JAR
     *
     * The servlet attempts to invoke the local EJB.
     * There is no virtual security domain configuration specified.
     */
    @Test
    @OperateOnDeployment(NO_OUTFLOW_CONFIG)
    public void testServletToEjbSingleDeploymentOutflowNotConfigured() throws Exception {
        String expectedMessage = "anonymous,false"; // alice won't be outflowed, the target security domain's current identity will be used instead
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage, true, url.toURI().resolve("whoAmI"));
    }

    /**
     * The EAR used in this test case contains:
     * - a servlet within a WAR
     * - a local EJB within a JAR
     *
     * The servlet attempts to invoke the local EJB.
     * outflow-anonymous has been configured for the virtual security domain that's being used to secure the servlet.
     */
    @Test
    @OperateOnDeployment(OUTFLOW_ANONYMOUS_CONFIG)
    public void testServletToEjbSingleDeploymentOutflowAnonymousConfigured() throws Exception {
        String expectedMessage = "anonymous,false"; // charlie won't be outflowed, anonymous will be used
        loginToApp(KeycloakConfiguration.CHARLIE, KeycloakConfiguration.CHARLIE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage, true, url.toURI().resolve("whoAmI"));
    }

    /**
     * Servlet -> EJB -> EJB propagation scenarios involving different security domains across deployments.
     */

    /**
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - a servlet within a WAR
     * - an EJB within a JAR
     *
     * EAR #2 contains:
     * - a local EJB within a JAR
     *
     * The servlet invokes the EJB from EAR #1 and that EJB attempts to invoke the local EJB in EAR #2.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_LOCAL)
    public void testEarToEarLocal() throws Exception {
        testServletToEjbToEjbInvocation();
    }

    /**
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - a servlet within a WAR
     * - an EJB within a JAR
     *
     * EAR #2 contains:
     * - a remote EJB within a JAR
     *
     * The servlet invokes the EJB from EAR #1 and that EJB attempts to invoke the remote EJB in EAR #2.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE)
    public void testEarToEarRemote() throws Exception {
        testServletToEjbToEjbInvocation();
    }

    /**
     * The servlet from EAR #1 is accessed using the wrong password.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE)
    public void testWrongPasswordEarToEar() throws Exception {
        testWrongPassword();
    }

    private void testServletToEjbToEjbInvocation() throws Exception {
        String expectedMessage = "alice,true"; // alice should have Managers role
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage,
                true, url.toURI().resolve("whoAmI"));
    }

    private void testServletToEjbToEjbInvocationSameDomain() throws Exception {
        String expectedMessage = "alice,true,false"; // alice should have user role and not Managers role
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, expectedMessage,
                true, url.toURI().resolve("whoAmI"));
    }

    /**
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - a servlet within a WAR
     *
     * EAR #2 contains:
     * - a remote EJB within a JAR
     *
     * The servlet invokes the remote EJB from EAR #2.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_REMOTE)
    public void testServletToEjbEarToEarRemote() throws Exception {
        testServletToEjbInvocation();
    }

    /**
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - a servlet within a WAR
     *
     * EAR #2 contains:
     * - a local EJB within a JAR
     *
     * The servlet invokes the local EJB from EAR #2.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_LOCAL)
    public void testServletToEjbEarToEarLocal() throws Exception {
        testServletToEjbInvocation();
    }

    /**
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - a servlet within a WAR
     * - an EJB within a JAR
     *
     * EAR #2 contains:
     * - a remote EJB within a JAR
     *
     * The servlet invokes the EJB from EAR #1 and that EJB attempts to invoke the remote EJB in EAR #2.
     * The EJB in EAR #2 is secured using the same virtual security domain as EAR #1.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN)
    public void testEarToEarRemoteSameDomain() throws Exception {
        testServletToEjbToEjbInvocationSameDomain();
    }

    static class EJBDomainSetupOverride extends ElytronDomainSetup {
        public EJBDomainSetupOverride() {
            super(new File(OidcIdentityPropagationTestCase.class.getResource("ejbusers.properties").getFile()).getAbsolutePath(),
                    new File(OidcIdentityPropagationTestCase.class.getResource("ejbroles.properties").getFile()).getAbsolutePath(),
                    EJB_SECURITY_DOMAIN_NAME);
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            super.setup(managementClient, containerId);
        }
    }

    static class AnotherEJBDomainSetupOverride extends ElytronDomainSetup {
        public AnotherEJBDomainSetupOverride() {
            super(new File(OidcIdentityPropagationTestCase.class.getResource("ejbusers.properties").getFile()).getAbsolutePath(),
                    new File(OidcIdentityPropagationTestCase.class.getResource("ejbroles.properties").getFile()).getAbsolutePath(),
                    ANOTHER_EJB_SECURITY_DOMAIN_NAME);
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            super.setup(managementClient, containerId);
        }
    }

    static class PropagationSetup extends ExtendedSnapshotServerSetupTask {

        @Override
        public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

            builder.addStep(getAddEjbApplicationSecurityDomainOp(EJB_SECURITY_DOMAIN_NAME, EJB_SECURITY_DOMAIN_NAME));
            builder.addStep(getAddEjbApplicationSecurityDomainOp(ANOTHER_EJB_SECURITY_DOMAIN_NAME, ANOTHER_EJB_SECURITY_DOMAIN_NAME));

            // /subsystem=elytron/virtual-security-domain=APP_NAME:add(outflow-security-domains=["ejb-domain"])
            // /subsystem=elytron/virtual-security-domain=another-single-deployment-local:add(outflow-security-domains=["another-ejb-domain"])
            for (String app : APP_NAMES) {
                builder.addStep(getAddVirtualSecurityDomainOp(app, app.equals(ANOTHER_SINGLE_DEPLOYMENT_LOCAL) ? ANOTHER_EJB_SECURITY_DOMAIN_NAME : EJB_SECURITY_DOMAIN_NAME));
            }

            // /subsystem=elytron/virtual-security-domain=outflow-anonymous-config.ear:write-attribute(name=outflow-anonymous, value=true)
            ModelNode op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "elytron");
            op.get(OP_ADDR).add("virtual-security-domain", OUTFLOW_ANONYMOUS_CONFIG + ".ear");
            op.get("name").set("outflow-anonymous");
            op.get("value").set(true);
            builder.addStep(op);

            // /subsystem=elytron/security-domain=ejb-domain:write-attribute(name=trusted-virtual-security-domains, value=["APP_NAMES"])
            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "elytron");
            op.get(OP_ADDR).add("security-domain", EJB_SECURITY_DOMAIN_NAME);
            op.get("name").set("trusted-virtual-security-domains");
            ModelNode trustedDomains = new ModelNode();
            for (String app : APP_NAMES) {
                trustedDomains.add(app + ".ear");
            }
            op.get("value").set(trustedDomains);
            builder.addStep(op);

            executeOperation(managementClient, builder.build());
        }

        @Override
        protected long timeout() {
            return TimeoutUtil.adjust(50);
        }

        private static PathAddress getEjbApplicationSecurityDomainAddress(String ejbDomainName) {
            return PathAddress.pathAddress()
                    .append(SUBSYSTEM, "ejb3")
                    .append("application-security-domain", ejbDomainName);
        }

        private static ModelNode getAddEjbApplicationSecurityDomainOp(String ejbDomainName, String securityDomainName) {
            ModelNode op = createAddOperation(getEjbApplicationSecurityDomainAddress(ejbDomainName));
            op.get("security-domain").set(securityDomainName);
            return op;
        }
    }

    static class KeycloakAndSubsystemSetup extends OidcBaseTest.KeycloakSetup {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
            OidcBaseTest.sendRealmCreationRequest(getRealmRepresentation(TEST_REALM, CLIENT_SECRET, CLIENT_HOST_NAME, CLIENT_PORT, CLIENT_IDS));

            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode("system-property=" + OIDC_PROVIDER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
            Utils.applyUpdate(operation, client);

            client = managementClient.getControllerClient();
            operation = createOpNode(PROVIDER_ADDRESS + KEYCLOAK_PROVIDER , ModelDescriptionConstants.ADD);
            operation.get("provider-url").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TEST_REALM);
            Utils.applyUpdate(operation, client);

            for (String app : CLIENT_IDS.keySet()) {
                operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + app + ".war", ModelDescriptionConstants.ADD);
                operation.get("client-id").set(app);
                operation.get("public-client").set(false);
                operation.get("provider").set(KEYCLOAK_PROVIDER);
                operation.get("ssl-required").set("EXTERNAL");
                operation.get("principal-attribute").set("preferred_username");
                Utils.applyUpdate(operation, client);

                operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + app + ".war/credential=secret", ModelDescriptionConstants.ADD);
                operation.get("secret").set("secret");
                Utils.applyUpdate(operation, client);
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode operation = createOpNode("system-property=" + OIDC_PROVIDER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            for (String appName : CLIENT_IDS.keySet()) {
                removeSecureDeployment(client, appName);
            }
            removeProvider(client, KEYCLOAK_PROVIDER);

            RestAssured
                    .given()
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .when()
                    .delete(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms/" + TEST_REALM).then().statusCode(204);

            super.tearDown(managementClient, containerId);
        }
    }

    private static void removeSecureDeployment(ModelControllerClient client, String name) throws Exception {
        ModelNode operation = createOpNode(SECURE_DEPLOYMENT_ADDRESS + name + ".war", ModelDescriptionConstants.REMOVE);
        Utils.applyUpdate(operation, client);
    }

    private static void removeProvider(ModelControllerClient client, String provider) throws Exception {
        ModelNode operation = createOpNode(PROVIDER_ADDRESS + provider, ModelDescriptionConstants.REMOVE);
        Utils.applyUpdate(operation, client);
    }

    private static PathAddress getVirtualSecurityDomainAddress(String virtualSecurityDomainName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("virtual-security-domain", virtualSecurityDomainName + ".ear");
    }

    private static ModelNode getAddVirtualSecurityDomainOp(String virtualSecurityDomainName, String... outflowDomains) {
        ModelNode op = createAddOperation(getVirtualSecurityDomainAddress(virtualSecurityDomainName));
        if (! virtualSecurityDomainName.equals(EAR_DEPLOYMENT_WITH_SERVLET_AND_EJB_REMOTE_SAME_DOMAIN)) {
            ModelNode outflowSecurityDomains = new ModelNode();
            for (String outflowSecurityDomain : outflowDomains) {
                outflowSecurityDomains.add(outflowSecurityDomain);
            }
            op.get("outflow-security-domains").set(outflowSecurityDomains);
        }
        return op;
    }

}
