/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.httpobfuscatedroute;

import java.net.SocketPermission;
import java.net.URL;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.remote.http.EchoRemote;
import org.jboss.as.test.integration.ejb.remote.http.HttpInvocationInContainerTestCase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Copy of {@link HttpInvocationInContainerTestCase} (cannot be extended because of class
 * finding issues with deployment classpath.
 *
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@ServerSetup(UndertowObfuscatedSessionRouteServerSetup.class)
public class HttpObfuscatedRouteInvocationInContainerTestCase  {
    @BeforeClass
    @AfterClass
    public static void checkFailure() throws Throwable {
        if (UndertowObfuscatedSessionRouteServerSetup.getFailure() != null) {
            throw UndertowObfuscatedSessionRouteServerSetup.getFailure();
        }
    }

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();

        return ShrinkWrap.create(WebArchive.class, "http-test.war")
                .addPackage(EchoRemote.class.getPackage())
                .addPackage(UndertowObfuscatedSessionRouteServerSetup.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new SocketPermission(SERVER_HOST_PORT, "connect,resolve")
                ), "permissions.xml");
    }

    @Test
    public void invokeEjb() throws NamingException {
        Hashtable table = new Hashtable();
        table.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        table.put(Context.PROVIDER_URL, "http://" + url.getHost() + ":" + url.getPort() + "/wildfly-services");
        table.put(Context.SECURITY_PRINCIPAL, "user1");
        table.put(Context.SECURITY_CREDENTIALS, "password1");
        InitialContext ic = new InitialContext(table);
        EchoRemote echo = (EchoRemote) ic.lookup("http-test/EchoBean!org.jboss.as.test.integration.ejb.remote.http.EchoRemote");
        Assert.assertEquals("hello", echo.echo("hello"));
    }
}
