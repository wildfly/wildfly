/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.ejb;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.ejb.client.RequestSendFailedException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.EjbElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Test authentication with the use of a source address role decoder where the IP address of the remote
 * client does not match the address configured on the decoder.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ AuthenticationWithSourceAddressRoleDecoderMismatchTestCase.ElytronDomainSetupOverride.class, EjbElytronDomainSetup.class, ServletElytronDomainSetup.class })
@Category(CommonCriteria.class)
public class AuthenticationWithSourceAddressRoleDecoderMismatchTestCase {

    private static final String APPLICATION_NAME = "authentication-with-source-address-role-decoder";

    @ArquillianResource
    private ManagementClient mgmtClient;

    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APPLICATION_NAME + ".jar");
        jar.addClasses(SecurityInformation.class, SecuredBean.class);
        return jar;
    }

    /*
     The Jakarta Enterprise Beans being used in this test class is secured using the "elytron-tests" security domain. This security
     domain is configured with:
      1) a source-address-role-decoder that assigns the "admin" role if the IP address of the remote client is 999.999.999.999
      2) a permission-mapper that assigns the "LoginPermission" if the identity has the "admin" role unless the principal
         is "user2"
     */

    @Test
    public void testAuthenticationIPAddressMismatch() throws Exception {
        Properties ejbClientConfiguration = createEjbClientConfiguration(Utils.getHost(mgmtClient), "user1", "password1");
        SecurityInformation targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class, ejbClientConfiguration);
        try {
            targetBean.getPrincipalName();
            Assert.fail("Expected RequestSendFailedException not thrown");
        } catch (RequestSendFailedException expected) {
        }

        ejbClientConfiguration = createEjbClientConfiguration(Utils.getHost(mgmtClient), "user2", "password2");
        targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class, ejbClientConfiguration);
        try {
            targetBean.getPrincipalName();
            Assert.fail("Expected RequestSendFailedException not thrown");
        } catch (RequestSendFailedException expected) {
        }
    }

    @Test
    public void testAuthenticationIPAddressMismatchWithSecurityRealmRole() throws Exception {
        final Properties ejbClientConfiguration = createEjbClientConfiguration(Utils.getHost(mgmtClient), "admin", "admin");
        final SecurityInformation targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class, ejbClientConfiguration);
        Assert.assertEquals("admin", targetBean.getPrincipalName());
    }

    @Test
    public void testAuthenticationInvalidCredentials() throws Exception {
        Properties ejbClientConfiguration = createEjbClientConfiguration(Utils.getHost(mgmtClient), "user1", "badpassword");
        SecurityInformation targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class, ejbClientConfiguration);
        try {
            targetBean.getPrincipalName();
            Assert.fail("Expected RequestSendFailedException not thrown");
        } catch (RequestSendFailedException expected) {
        }

        ejbClientConfiguration = createEjbClientConfiguration(Utils.getHost(mgmtClient), "user2", "badpassword");
        targetBean = lookupEJB(SecuredBean.class, SecurityInformation.class, ejbClientConfiguration);
        try {
            targetBean.getPrincipalName();
            Assert.fail("Expected RequestSendFailedException not thrown");
        } catch (RequestSendFailedException expected) {
        }
    }

    private static Properties createEjbClientConfiguration(String hostName, String user, String password) {
        final Properties pr = new Properties();
        pr.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        pr.put("remote.connection.default.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
        pr.put("remote.connections", "default");
        pr.put("remote.connection.default.host", hostName);
        pr.put("remote.connection.default.port", "8080");
        pr.put("remote.connection.default.username", user);
        pr.put("remote.connection.default.password", password);
        return pr;
    }

    private static <T> T lookupEJB(Class<? extends T> beanImplClass, Class<T> remoteInterface, Properties ejbProperties) throws Exception {
        final Properties jndiProperties = new Properties();
        jndiProperties.putAll(ejbProperties);
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);

        return (T) context.lookup("ejb:/" + APPLICATION_NAME + "/" + beanImplClass.getSimpleName() + "!"
                + remoteInterface.getName());
    }

    static class ElytronDomainSetupOverride extends ElytronDomainSetup {
        public ElytronDomainSetupOverride() {
            super(new File(AuthenticationWithSourceAddressRoleDecoderMismatchTestCase.class.getResource("users.properties").getFile()).getAbsolutePath(),
                    new File(AuthenticationWithSourceAddressRoleDecoderMismatchTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath(),
                    "elytron-tests",
                    "ipPermissionMapper",
                    "999.999.999.999");
        }
    }

}
