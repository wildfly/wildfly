/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.security;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * A test case to test an unsecured EJB setting the username and password before the call reaches a secured EJB.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpRemoteIdentityTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private static AuthenticationContext old;

    @BeforeClass
    public static void setup() {
        AuthenticationConfiguration config = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, config);
        old = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(context);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(old);
    }

    /**
     * Creates a deployment application for this test.
     *
     * @return
     * @throws IOException
     */
    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EJBUtil.APPLICATION_NAME + ".jar");
        jar.addClasses(SecurityInformation.class, IntermediateAccess.class, EntryBean.class, SecuredBean.class, Util.class);
        return jar;
    }

    @Test
    public void testDirect() throws Exception {
        final SecurityInformation targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class);

        assertEquals("user1", targetBean.getPrincipalName());
    }

    @Test
    public void testUnsecured() throws Exception {
        final IntermediateAccess targetBean = lookupEJB(EntryBean.class, IntermediateAccess.class);

        assertEquals("anonymous", targetBean.getPrincipalName());
    }

    private Context getRemoteHTTPContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        URI namingUri = getHttpUri();
        env.put(Context.PROVIDER_URL, namingUri.toString());
        return new InitialContext(env);
    }

    private URI getHttpUri() throws URISyntaxException {
        URI webUri = managementClient.getWebUri();
        return new URI("http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "", "");
    }

    private <T> T lookupEJB(Class<? extends T> beanImplClass, Class<T> remoteInterface) throws Exception {
        final Context context = getRemoteHTTPContext();
        return (T) context.lookup("ejb:/ejb-remote-security-test/" + beanImplClass.getSimpleName() + "!"
                + remoteInterface.getName());
    }

}
