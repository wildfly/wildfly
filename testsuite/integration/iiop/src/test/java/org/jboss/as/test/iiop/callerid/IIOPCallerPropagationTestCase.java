/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.callerid;

import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Tests CSIv2 identity assertion (caller propagation) enforcement without TLS.
 *
 * The server runs with {@code caller-propagation-require-auth="false"}, disabling
 * the {@code SASTargetInterceptor} enforcement. Three server beans are deployed:
 * <ul>
 *   <li>{@code CallerPropBean} — advertises GSSUP ({@code as-context required="true"})
 *       and {@code caller-propagation="supported"}. The client sends both GSSUP credentials
 *       and an identity token. Succeeds because GSSUP provides authentication.</li>
 *   <li>{@code CallerPropNoGssupBean} — uses {@code @SecurityDomain("iiopCert")} which
 *       grants {@code RunAsPrincipalPermission("*")} to anonymous. Identity assertion
 *       without authentication succeeds because the domain permits anonymous run-as.</li>
 *   <li>{@code CallerPropDefaultDomainBean} — uses the default {@code ApplicationDomain}
 *       which does NOT grant {@code RunAsPrincipalPermission} to anonymous. Identity
 *       assertion without authentication fails at the EJB layer.</li>
 * </ul>
 */
@RunWith(Arquillian.class)
public class IIOPCallerPropagationTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "server.jar");
        jar.addClasses(CallerPropBean.class, CallerPropNoGssupBean.class,
                        CallerPropDefaultDomainBean.class,
                        CallerPropHome.class, CallerPropRemote.class)
                .addAsManifestResource(IIOPCallerPropagationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {
        String ejbJar = FileUtils.readFile(IIOPCallerPropagationTestCase.class, "ejb-jar.xml");

        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        jar.addClasses(ClientEjb.class, CallerPropHome.class, CallerPropRemote.class,
                        IIOPCallerPropagationTestCase.class, Util.class)
                .addAsManifestResource(IIOPCallerPropagationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getSecurityDomain"), new ElytronPermission("getPrivateCredentials")), "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testCallerPropagationWithGssupSucceeds() throws Exception {
        final String result = Util.switchIdentity("user1", "password1",
                (Callable<String>) () -> client().callGssupServer());
        Assert.assertEquals("hello", result);
    }

    @Test
    @OperateOnDeployment("client")
    public void testAnonymousCallerPropagationWithPermissionSucceeds() throws Exception {
        final String result = Util.switchIdentity("user1", "password1",
                (Callable<String>) () -> client().callNoGssupServer());
        Assert.assertEquals("hello", result);
    }

    @Test
    @OperateOnDeployment("client")
    public void testAnonymousCallerPropagationWithoutPermissionFails() throws Exception {
        try {
            Util.switchIdentity("user1", "password1", (Callable<Void>) () -> {
                client().callDefaultDomainServer();
                return null;
            });
            Assert.fail("Expected anonymous identity assertion to fail without RunAsPrincipalPermission");
        } catch (Exception expected) {
        }
    }

    private ClientEjb client() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
