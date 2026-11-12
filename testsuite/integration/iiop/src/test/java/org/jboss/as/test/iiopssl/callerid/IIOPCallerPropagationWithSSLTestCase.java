/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.callerid;

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
 * Tests CSIv2 identity assertion (caller propagation) enforcement with TLS.
 *
 * Three server beans are deployed, all requiring TLS mutual authentication
 * and supporting caller propagation:
 * <ul>
 *   <li>{@code CallerPropBean} — uses the {@code iiopCert} security domain which
 *       can verify X.509 client certificate evidence. Identity assertion succeeds.</li>
 *   <li>{@code CallerPropDefaultDomainBean} — uses the default security domain
 *       (ApplicationDomain) which cannot verify X.509 evidence. TLS handshake
 *       succeeds (the client certificate is trusted), but evidence verification
 *       in {@code EjbCorbaServant} fails since the client sends no GSSUP credentials
 *       for this bean.</li>
 *   <li>{@code CallerPropGssupAndTlsBean} — uses the {@code gssupRunAs} security domain,
 *       which can authenticate GSSUP and grants {@code RunAsPrincipalPermission} but, like
 *       the default domain, cannot verify X.509 evidence. It additionally requires GSSUP
 *       ({@code as-context required="true"}). The client therefore sends both a TLS client
 *       certificate and GSSUP credentials; since the domain cannot verify the certificate,
 *       the GSSUP credentials must be used to authenticate the asserting entity for the
 *       call to succeed. It is deployed separately from the other two beans since a single
 *       deployment unit may only be associated with one explicit Elytron security domain
 *       (see {@code WFLYEJB0490}).</li>
 * </ul>
 */
@RunWith(Arquillian.class)
public class IIOPCallerPropagationWithSSLTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "server.jar");
        jar.addClasses(CallerPropBean.class, CallerPropDefaultDomainBean.class,
                        CallerPropHome.class, CallerPropRemote.class)
                .addAsManifestResource(IIOPCallerPropagationWithSSLTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "gssup-server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> gssupDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "gssup-server.jar");
        jar.addClasses(CallerPropGssupAndTlsBean.class, CallerPropHome.class, CallerPropRemote.class)
                .addAsManifestResource(IIOPCallerPropagationWithSSLTestCase.class.getPackage(), "jboss-ejb3-gssup.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {
        String ejbJar = FileUtils.readFile(IIOPCallerPropagationWithSSLTestCase.class, "ejb-jar.xml");

        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        jar.addClasses(ClientEjb.class, CallerPropHome.class, CallerPropRemote.class,
                        IIOPCallerPropagationWithSSLTestCase.class, Util.class)
                .addAsManifestResource(IIOPCallerPropagationWithSSLTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getSecurityDomain"), new ElytronPermission("getPrivateCredentials")), "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testCallerPropagationWithTlsSucceeds() throws Exception {
        final String result = Util.switchIdentity("user1", "password1",
                (Callable<String>) () -> client().callServer());
        Assert.assertEquals("hello", result);
    }

    @Test
    @OperateOnDeployment("client")
    public void testCallerPropagationWithWrongSecurityDomainFails() throws Exception {
        try {
            Util.switchIdentity("user1", "password1", (Callable<Void>) () -> {
                client().callDefaultDomainServer();
                return null;
            });
            Assert.fail("Expected identity propagation to fail when server security domain cannot verify TLS certificate");
        } catch (Exception expected) {
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testCallerPropagationWithGssupAndTlsSucceeds() throws Exception {
        // the client presents a TLS client certificate (the domain cannot verify it) *and* GSSUP
        // credentials. The GSSUP credentials must still be used to authenticate the asserting
        // entity, so the call succeeds despite the certificate being unverifiable by this domain.
        final String result = Util.switchIdentity("user1", "password1",
                (Callable<String>) () -> client().callGssupAndTlsServer());
        Assert.assertEquals("hello", result);
    }

    private ClientEjb client() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
