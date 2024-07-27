/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.microprofile.jwt.TokenUtil.generateJWT;

import java.net.URL;
import java.nio.file.Paths;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.jwt.BaseJWTCase;

/**
 * A test case for a Jakarta Enterprise Bean endpoint secured using the MP-JWT mechanism and invoking a
 * second Jakarta Enterprise Bean within the same deployment and across deployments.
 *
 * @author <a href="fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ JWTIdentityPropagationTestCase.PropagationSetup.class })
public class JWTIdentityPropagationTestCase {

    private static final String SINGLE_DEPLOYMENT = "single-deployment";
    private static final String ANOTHER_SINGLE_DEPLOYMENT = "another-single-deployment";
    private static final String NO_OUTFLOW_CONFIG = "no-outflow-config";
    private static final String OUTFLOW_ANONYMOUS_CONFIG = "outflow-anonymous-config";
    private static final String EAR_DEPLOYMENT_WITH_MP_JWT = "ear-deployment-mp-jwt";
    private static final String EAR_DEPLOYMENT_WITH_EJB = "ear-deployment-ejb";
    private static final String EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN = "ear-deployment-mp-jwt-same-domain";
    private static final String EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN = "ear-deployment-ejb-same-domain";
    private static final URL KEY_LOCATION = BaseJWTCase.class.getResource("private.pem");

    private static final String ROOT_PATH = "/rest/Sample/";
    private static final String ANOTHER_ROOT_PATH = "/rest/AnotherSample/";
    private static final String SUBSCRIPTION = "subscription";

    private static final String DATE = "2017-09-15";
    private static final String ECHOER_GROUP = "Echoer";
    private static final String SUBSCRIBER_GROUP = "Subscriber";

    private static final String PRINCIPAL_NAME = "testUser";
    private static final String NON_EXISTING_PRINCIPAL_NAME = "nonExistingUser";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    private static final String EJB_SECURITY_DOMAIN = "BusinessDomain";
    private static final String EJB_SECURITY_REALM = "BusinessRealm";
    private static final String EJB_SECURITY_REALM_PATH = "business-realm-users";
    private static final String EJB_SECURITY_REALM_USER = "testUser";
    private static final String EJB_SECURITY_REALM_ADMIN_ROLE = "Admin";

    private static final String ANOTHER_EJB_SECURITY_DOMAIN = "AnotherBusinessDomain";
    private static final String ANOTHER_EJB_SECURITY_REALM = "AnotherBusinessRealm";
    private static final String ANOTHER_EJB_SECURITY_REALM_PATH = "another-business-realm-users";

    private final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    @ArquillianResource
    private URL deploymentUrl;

    @Deployment(name= SINGLE_DEPLOYMENT, order = 1)
    public static Archive<?> singleDeploymentLocal() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SINGLE_DEPLOYMENT + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, BeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLE_DEPLOYMENT + "-ejb.jar")
                .addClasses(TargetBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SINGLE_DEPLOYMENT + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= ANOTHER_SINGLE_DEPLOYMENT, order = 2)
    public static Archive<?> anotherSingleDeploymentLocal() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ANOTHER_SINGLE_DEPLOYMENT + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, AnotherBeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ANOTHER_SINGLE_DEPLOYMENT + "-ejb.jar")
                .addClasses(AnotherTargetBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ANOTHER_SINGLE_DEPLOYMENT + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= NO_OUTFLOW_CONFIG, order = 3)
    public static Archive<?> noOutflowConfig() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, NO_OUTFLOW_CONFIG + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, BeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, NO_OUTFLOW_CONFIG + "-ejb.jar")
                .addClasses(TargetBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, NO_OUTFLOW_CONFIG + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= OUTFLOW_ANONYMOUS_CONFIG, order = 4)
    public static Archive<?> outflowAnonymousConfig() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, OUTFLOW_ANONYMOUS_CONFIG + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, BeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, OUTFLOW_ANONYMOUS_CONFIG + "-ejb.jar")
                .addClasses(TargetBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, OUTFLOW_ANONYMOUS_CONFIG + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_EJB, order = 5)
    public static Archive<?> earDeploymentWithEJB() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_EJB + "-ejb.jar")
                .addClasses(WhoAmIRemote.class)
                .addClasses(WhoAmIBeanRemote.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_EJB + ".ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_MP_JWT, order = 6)
    public static Archive<?> earDeploymentWithMPJWT() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_MP_JWT + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, RemoteBeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_MP_JWT + ".ear");
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN, order = 7)
    public static Archive<?> earDeploymentWithEJBSameDomain() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN + "-ejb.jar")
                .addClasses(org.wildfly.test.integration.microprofile.jwt.propagation.sameVirtualDomain.WhoAmIRemote.class)
                .addClasses(org.wildfly.test.integration.microprofile.jwt.propagation.sameVirtualDomain.WhoAmIBeanRemote.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN + ".ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name= EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN, order = 8)
    public static Archive<?> earDeploymentWithMPJWTSameDomain() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN + "-web.war")
                .addClasses(JWTIdentityPropagationTestCase.class)
                .addClasses(App.class, org.wildfly.test.integration.microprofile.jwt.propagation.sameVirtualDomain.RemoteBeanEndPoint.class)
                .addClasses(PropagationSetup.class)
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem")
                .addAsManifestResource(new StringAsset("Dependencies: deployment." + EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN + ".ear" + "." + EAR_DEPLOYMENT_WITH_EJB_SAME_DOMAIN + "-ejb.jar"), "MANIFEST.MF");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN + ".ear");
        ear.addAsModule(war);
        return ear;
    }


    /**
     * Identity propagation scenarios within a single deployment.
     */

    /*
     * The EAR used in this test case contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     * - A second Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from the WAR attempts to invoke the second Jakarta Enterprise Bean.
     * The invocation should succeed.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT)
    public void testInvokeEJBWithinSingleDeployment() throws Exception {
       testInvokeEJB(ROOT_PATH, PRINCIPAL_NAME, getExpectedMessage(PRINCIPAL_NAME, true));
    }

    /*
     * The EAR used in this test case contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     * - A second Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from the WAR attempts to invoke the second Jakarta Enterprise Bean.
     * The identity being used to invoke the endpoint doesn't exist in the security
     * domain that's being used to secure the second Jakarta Enterprise Bean.
     */
    @Test
    @OperateOnDeployment(SINGLE_DEPLOYMENT)
    public void testInvokeEJBWithinSingleDeploymentOutflowNotPossibleNonExistentIdentity() throws Exception {
        testInvokeEJB(ROOT_PATH, NON_EXISTING_PRINCIPAL_NAME, getExpectedMessage("anonymous", false));
    }

    /*
     * The EAR used in this test case contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     * - A second Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from the WAR attempts to invoke the second Jakarta Enterprise Bean.
     * The security domain that's being used to secure the second Jakarta Enterprise Bean
     * hasn't been configured to trust the virtual security domain.
     */
    @Test
    @OperateOnDeployment(ANOTHER_SINGLE_DEPLOYMENT)
    public void testInvokeEJBWithinSingleDeploymentOutflowNotPossibleTrustNotConfigured() throws Exception {
        testInvokeEJB(ANOTHER_ROOT_PATH, PRINCIPAL_NAME, getExpectedMessage("anonymous", false));
    }

    /*
     * The EAR used in this test case contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     * - A second Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from the WAR attempts to invoke the second Jakarta Enterprise Bean.
     * There is no virtual security domain configuration specified.
     */
    @Test
    @OperateOnDeployment(NO_OUTFLOW_CONFIG)
    public void testInvokeEJBWithinSingleDeploymentOutflowNotConfigured() throws Exception {
        testInvokeEJB(ROOT_PATH, PRINCIPAL_NAME, getExpectedMessage("anonymous", false));
    }

    /*
     * The EAR used in this test case contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     * - A second Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from the WAR attempts to invoke the second Jakarta Enterprise Bean.
     * outflow-anonymous has been configured for the virtual security domain.
     */
    @Test
    @OperateOnDeployment(OUTFLOW_ANONYMOUS_CONFIG)
    public void testInvokeEJBWithinSingleDeploymentOutflowAnonymousConfigured() throws Exception {
        testInvokeEJB(ROOT_PATH, PRINCIPAL_NAME, getExpectedMessage("anonymous", false));
    }

    /**
     * Identity propagation scenarios across deployments.
     */

    /*
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     *
     * EAR #2 contains:
     * - A Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from EAR #1 attempts to invoke the Jakarta Enterprise Bean in EAR #2.
     * The invocation should succeed.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_MP_JWT)
    public void testInvokeEJBInAnotherEAR() throws Exception {
        testInvokeEJB(ROOT_PATH, PRINCIPAL_NAME, getExpectedMessage(PRINCIPAL_NAME, true));
    }

    /*
     * Two EARs are used in this test case.
     * EAR #1 contains:
     * - A Jakarta Enterprise Bean endpoint secured with MP-JWT located within a WAR
     *
     * EAR #2 contains:
     * - A Jakarta Enterprise Bean in a JAR
     *
     * The endpoint from EAR #1 attempts to invoke the Jakarta Enterprise Bean in EAR #2.
     * The Jakarta Enterprise Bean in EAR #2 is secured using the same virtual domain as EAR #1.
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN)
    public void testInvokeEJBInAnotherEARSameDomain() throws Exception {
        testInvokeEJB(ROOT_PATH, PRINCIPAL_NAME, "targetCallerPrincipal: " + PRINCIPAL_NAME + ", hasAdminRole: false, hasSubscriberRole: true");
    }

    private void testInvokeEJB(String rootPath, String principalName, String expectedMessage) throws Exception {
        String jwtToken = generateJWT(Paths.get(KEY_LOCATION.toURI()).toAbsolutePath().toString(), principalName, DATE, ECHOER_GROUP, SUBSCRIBER_GROUP);

        HttpGet httpGet = new HttpGet(deploymentUrl.toString() + rootPath + SUBSCRIPTION);
        httpGet.addHeader(AUTHORIZATION, BEARER + " " + jwtToken);

        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("Successful call", 200, httpResponse.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(httpResponse.getEntity());
        // rls tmp disable   assertTrue("Call was authenticated", body.contains(expectedMessage));
        assertTrue("expectedMessage:["+ expectedMessage +
                "] returned msg:[" +body+ "]", body.contains(expectedMessage)); // rls debug
        httpResponse.close();
    }

    private String getExpectedMessage(String expectedUser, boolean shouldHaveAdminRole) {
        return "targetCallerPrincipal: " + expectedUser + ", targetIsCallerAdmin: " + shouldHaveAdminRole;
    }

    static class PropagationSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli =  new CLIWrapper(true)) {
                rlsSetupDebugLog(cli); // rls tmp debug
                createFilesystemRealm(cli, EJB_SECURITY_DOMAIN, EJB_SECURITY_REALM, EJB_SECURITY_REALM_PATH, EJB_SECURITY_REALM_USER, EJB_SECURITY_REALM_ADMIN_ROLE);
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:add(auth-method=MP-JWT,outflow-security-domains=[%s])",
                        SINGLE_DEPLOYMENT + ".ear", EJB_SECURITY_DOMAIN)); // outflow config (corresponding trust config is below)

                createFilesystemRealm(cli, ANOTHER_EJB_SECURITY_DOMAIN, ANOTHER_EJB_SECURITY_REALM, ANOTHER_EJB_SECURITY_REALM_PATH, EJB_SECURITY_REALM_USER, EJB_SECURITY_REALM_ADMIN_ROLE);
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:add(auth-method=MP-JWT,outflow-security-domains=[%s])",
                        ANOTHER_SINGLE_DEPLOYMENT + ".ear", ANOTHER_EJB_SECURITY_DOMAIN)); // outflow config only

                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:add(auth-method=MP-JWT,outflow-security-domains=[%s], outflow-anonymous=true)",
                        OUTFLOW_ANONYMOUS_CONFIG + ".ear", EJB_SECURITY_DOMAIN)); // outflow config only

                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:add(auth-method=MP-JWT,outflow-security-domains=[%s], outflow-anonymous=true)",
                        EAR_DEPLOYMENT_WITH_MP_JWT + ".ear", EJB_SECURITY_DOMAIN)); // outflow config (corresponding trust config is below)

                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:write-attribute(name=trusted-virtual-security-domains, value=[%s, %s])",
                        EJB_SECURITY_DOMAIN, SINGLE_DEPLOYMENT + ".ear", EAR_DEPLOYMENT_WITH_MP_JWT + ".ear")); // trust config

                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:add(auth-method=MP-JWT)",
                        EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN + ".ear"));
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        // rls start
        private void rlsSetupDebugLog(CLIWrapper cli){
            cli.sendLine("/subsystem=logging/logger=org.wildfly.security:add()");
            cli.sendLine("/subsystem=logging/logger=org.wildfly.security:write-attribute(name=level, value=DEBUG)");
            cli.sendLine("/subsystem=logging/logger=org.apache.http:add()");
            cli.sendLine("/subsystem=logging/logger=org.apache.http:write-attribute(name=level, value=DEBUG)");
        }
        private void rlsTearDownDebugLog(CLIWrapper cli){
            cli.sendLine("/subsystem=logging/logger=org.wildfly.security:remove()");
            cli.sendLine("/subsystem=logging/logger=org.apache.http:remove()");
        }
        // rls end
        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:undefine-attribute(name=trusted-virtual-security-domains)",  EJB_SECURITY_DOMAIN));

                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:remove()", SINGLE_DEPLOYMENT + ".ear"));
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:remove()", ANOTHER_SINGLE_DEPLOYMENT + ".ear"));
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:remove()", OUTFLOW_ANONYMOUS_CONFIG + ".ear"));
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:remove()", EAR_DEPLOYMENT_WITH_MP_JWT + ".ear"));
                cli.sendLine(String.format("/subsystem=elytron/virtual-security-domain=%s:remove()", EAR_DEPLOYMENT_WITH_MP_JWT_SAME_DOMAIN + ".ear"));

                cli.sendLine(String.format("/subsystem=ejb3/application-security-domain=%s:remove()", EJB_SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=ejb3/application-security-domain=%s:remove()", ANOTHER_EJB_SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", EJB_SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", EJB_SECURITY_REALM));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", ANOTHER_EJB_SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", ANOTHER_EJB_SECURITY_REALM));

                rlsTearDownDebugLog(cli); // rls debug
            }
            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        private void createFilesystemRealm(CLIWrapper cli, String domainName, String realmName, String path, String user, String role) {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add(path=%s,relative-to=jboss.server.config.dir)",
                    realmName, path));
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity(identity=%s)", realmName, user));
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=Roles, value=[%s])",
                    realmName, user, role));
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s}],default-realm=%2$s,permission-mapper=default-permission-mapper)",
                    domainName, realmName));
            cli.sendLine(String.format("/subsystem=ejb3/application-security-domain=%s:add(security-domain=%s)",
                    domainName, domainName));
        }
    }
}
