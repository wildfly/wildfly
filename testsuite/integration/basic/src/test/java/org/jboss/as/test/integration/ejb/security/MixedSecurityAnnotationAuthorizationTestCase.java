package org.jboss.as.test.integration.ejb.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.authorization.DenyAllOverrideBean;
import org.jboss.as.test.integration.ejb.security.authorization.PermitAllOverrideBean;
import org.jboss.as.test.integration.ejb.security.authorization.RolesAllowedOverrideBean;
import org.jboss.as.test.integration.ejb.security.authorization.RolesAllowedOverrideBeanBase;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.elytron.EjbElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test case to test the general authorization requirements for annotated beans. The server setup has both
 * an application-security-domain backed by Elytron security domain and legacy security domain with the same name.
 *
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2017 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({MixedSecurityAnnotationAuthorizationTestCase.OverridenEjbSecurityDomainSetup.class,
        MixedSecurityAnnotationAuthorizationTestCase.OverridingElytronDomainSetup.class,
        MixedSecurityAnnotationAuthorizationTestCase.OverridingEjbElytronDomainSetup.class,
        MixedSecurityAnnotationAuthorizationTestCase.OverridingServletElytronDomainSetup.class})
public class MixedSecurityAnnotationAuthorizationTestCase {

    @Deployment
    public static Archive<?> runAsDeployment() {
        final Package currentPackage = AnnotationAuthorizationTestCase.class.getPackage();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addClasses(RolesAllowedOverrideBean.class, RolesAllowedOverrideBeanBase.class, PermitAllOverrideBean.class, DenyAllOverrideBean.class).addClass(Util.class)
                .addClasses(MixedSecurityAnnotationAuthorizationTestCase.class)
                .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class, ElytronDomainSetup.class, EjbElytronDomainSetup.class, ServletElytronDomainSetup.class)
                .addAsWebInfResource(currentPackage, "jboss-web.xml", "jboss-web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new ElytronPermission("getSecurityDomain"),
                new ElytronPermission("authenticate")
                ), "permissions.xml");
        war.addPackage(CommonCriteria.class.getPackage());
        return war;
    }

    @EJB(mappedName = "java:global/ejb3security/RolesAllowedOverrideBean")
    private RolesAllowedOverrideBean rolesAllowedOverridenBean;

    /*
     * Test overrides within a bean annotated @RolesAllowed at bean level.
     */

    @Test
    public void testRolesAllowedOverriden_NoUser() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        try {
            rolesAllowedOverridenBean.defaultEcho("1");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        try {
            rolesAllowedOverridenBean.denyAllEcho("2");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        String response = rolesAllowedOverridenBean.permitAllEcho("3");
        assertEquals("3", response);

        try {
            rolesAllowedOverridenBean.role2Echo("4");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testRolesAllowedOverriden_User1() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            String response = rolesAllowedOverridenBean.defaultEcho("1");
            assertEquals("1", response);

            try {
                rolesAllowedOverridenBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            response = rolesAllowedOverridenBean.permitAllEcho("3");
            assertEquals("3", response);

            try {
                rolesAllowedOverridenBean.role2Echo("4");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }
            return null;
        };
        runAsElytronIdentity("user1", "elytronPassword1", callable);
    }

    @Test
    public void testRolesAllowedOverridenInBaseClass_Admin() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            try {
                rolesAllowedOverridenBean.aMethod("aMethod");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            String response = rolesAllowedOverridenBean.bMethod("bMethod");
            assertEquals("bMethod", response);
            return null;
        };
        runAsElytronIdentity("admin", "elytronAdmin", callable);
    }

    @Test
    public void testRolesAllowedOverridenInBaseClass_HR() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            String response = rolesAllowedOverridenBean.aMethod("aMethod");
            assertEquals("aMethod", response);

            try {
                rolesAllowedOverridenBean.bMethod("bMethod");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }
            return null;
        };
        runAsElytronIdentity("hr", "elytronHr", callable);
    }

    @Test
    public void testRolesAllowedOverriden_User2() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            try {
                rolesAllowedOverridenBean.defaultEcho("1");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            try {
                rolesAllowedOverridenBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            String response = rolesAllowedOverridenBean.permitAllEcho("3");
            assertEquals("3", response);

            response = rolesAllowedOverridenBean.role2Echo("4");
            assertEquals("4", response);
            return null;
        };
        runAsElytronIdentity("user2", "elytronPassword2", callable);
    }

    /*
     * Test overrides of bean annotated at bean level with @PermitAll
     */

    @EJB(mappedName = "java:global/ejb3security/PermitAllOverrideBean")
    private PermitAllOverrideBean permitAllOverrideBean;

    @Test
    public void testPermitAllOverride_NoUser() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        String response = permitAllOverrideBean.defaultEcho("1");
        assertEquals("1", response);

        try {
            permitAllOverrideBean.denyAllEcho("2");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        try {
            permitAllOverrideBean.role1Echo("3");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testPermitAllOverride_User1() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            String response = permitAllOverrideBean.defaultEcho("1");
            assertEquals("1", response);

            try {
                permitAllOverrideBean.denyAllEcho("2");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            response = permitAllOverrideBean.role1Echo("3");
            assertEquals("3", response);
            return null;
        };
        runAsElytronIdentity("user1", "elytronPassword1", callable);
    }

    /*
     * Test overrides of ben annotated at bean level with @DenyAll
     */

    @EJB(mappedName = "java:global/ejb3security/DenyAllOverrideBean")
    private DenyAllOverrideBean denyAllOverrideBean;

    @Test
    public void testDenyAllOverride_NoUser() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        try {
            denyAllOverrideBean.defaultEcho("1");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }

        String response = denyAllOverrideBean.permitAllEcho("2");
        assertEquals("2", response);

        try {
            denyAllOverrideBean.role1Echo("3");
            fail("Expected EJBAccessException not thrown");
        } catch (EJBAccessException ignored) {
        }
    }

    @Test
    public void testDenyAllOverride_User1() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            try {
                denyAllOverrideBean.defaultEcho("1");
                fail("Expected EJBAccessException not thrown");
            } catch (EJBAccessException ignored) {
            }

            String response = denyAllOverrideBean.permitAllEcho("2");
            assertEquals("2", response);

            response = denyAllOverrideBean.role1Echo("3");
            assertEquals("3", response);
            return null;
        };
        runAsElytronIdentity("user1", "elytronPassword1", callable);
    }

    /**
     * Tests that a method which accepts an array as a parameter and is marked with @PermitAll is allowed to be invoked by clients.
     *
     * @throws Exception
     */
    @Test
    public void testPermitAllMethodWithArrayParams() throws Exception {
        Assert.assertNotNull("An Elytron security domain should be associated with test EJB deployment.", SecurityDomain.getCurrent());
        final Callable<Void> callable = () -> {
            final String[] messages = new String[] {"foo", "bar"};
            final String[] echoes = denyAllOverrideBean.permitAllEchoWithArrayParams(messages);
            assertArrayEquals("Unexpected echoes returned by bean method", messages, echoes);
            return null;
        };
        runAsElytronIdentity("user1", "elytronPassword1", callable);
    }

    private static <T> T runAsElytronIdentity(final String username, final String password, final Callable<T> callable) throws Exception {
        if (username != null && password != null) {
            final SecurityDomain securityDomain = SecurityDomain.getCurrent();
            final SecurityIdentity securityIdentity = securityDomain.authenticate(username, new PasswordGuessEvidence(password.toCharArray()));
            return securityIdentity.runAs(callable);
        }
        return callable.call();
    }

    public static class OverridingElytronDomainSetup extends ElytronDomainSetup {

        public OverridingElytronDomainSetup() {
            super(new File(MixedSecurityAnnotationAuthorizationTestCase.class.getResource("elytronusers.properties").getFile()).getAbsolutePath(),
                  new File(MixedSecurityAnnotationAuthorizationTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath());
        }

    }

    public static class OverridingEjbElytronDomainSetup extends EjbElytronDomainSetup {

        @Override
        protected String getEjbDomainName() {
            return "ejb3-tests";
        }

    }

    public static class OverridingServletElytronDomainSetup extends ServletElytronDomainSetup {

        @Override
        protected String getUndertowDomainName() {
            return "ejb3-tests";
        }

    }

    public static class OverridenEjbSecurityDomainSetup extends EjbSecurityDomainSetup {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();

            ModelNode steps = compositeOp.get(STEPS);
            PathAddress securityDomainAddress = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, getSecurityDomainName());
            steps.add(createAddOperation(securityDomainAddress));

            PathAddress authAddress = securityDomainAddress.append(AUTHENTICATION, Constants.CLASSIC);
            steps.add(createAddOperation(authAddress));

            ModelNode op = createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "Remoting"));
            op.get(CODE).set("Remoting");
            op.get(FLAG).set("optional");
            op.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            steps.add(op);

            ModelNode loginModule = createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "UsersRoles"));
            loginModule.get(CODE).set("UsersRoles");
            loginModule.get(FLAG).set("required");
            loginModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass")
                    .add("rolesProperties", getGroupsFile())
                    .add("usersProperties", getUsersFile());
            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(loginModule);

            applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
        }
    }

}
