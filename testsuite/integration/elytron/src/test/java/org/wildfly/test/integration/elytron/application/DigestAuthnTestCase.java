/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.application;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.http.client.ElytronHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpResponse;

@RunWith(Arquillian.class)
public class DigestAuthnTestCase {

    private static final String NAME = DigestAuthnTestCase.class.getSimpleName();

    /**
     * Creates WAR with a secured servlet and Digest authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class)
                .addAsWebInfResource(DigestAuthnTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    @Test
    public void testElytronHttpClientDigestAuth(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context1 = AuthenticationContext.empty().with(MatchRule.ALL.matchHost(servletUrl.getHost()), adminConfig);
        AuthenticationContext context2 = AuthenticationContext.empty().with(MatchRule.ALL.matchHost(servletUrl.getHost()), adminConfig);
        ElytronHttpClient elytronHttpClient = new ElytronHttpClient();
        context1.run(() -> {
            try{
                HttpResponse response = elytronHttpClient.connect(servletUrl.toString());
                Assert.assertEquals(SC_OKi,response.statusCode());
            }catch (Exception e){
                Assert.fail("Can not connect to Elytron Http client");
            }
        });
        context2.run(() -> {
            try{
                HttpResponse response = elytronHttpClient.connect(servletUrl.toString());
                Assert.assertEquals(SC_OK,response.statusCode());
            }catch (Exception e){
                Assert.fail("Can not connect to Elytron Http client");
            }
        });
    }

    @Test
    public void testElytronHttpClientDigestAuthInvalidUser(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user2").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL.matchHost(servletUrl.getHost()), adminConfig);
        ElytronHttpClient elytronHttpClient = new ElytronHttpClient();
        context.run(() -> {
            try{
                HttpResponse response = elytronHttpClient.connect(servletUrl.toString());
                Assert.assertEquals(SC_UNAUTHORIZED,response.statusCode());
            }catch (Exception e){
                Assert.fail("Can not connect to Elytron Http client");
            }
        });
    }

    @Test
    public void testElytronHttpClientDigestAuthUnsupportedRole(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role2");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL.matchHost(servletUrl.getHost()), adminConfig);
        ElytronHttpClient elytronHttpClient = new ElytronHttpClient();
        context.run(() -> {
            try{
                HttpResponse response = elytronHttpClient.connect(servletUrl.toString());
                Assert.assertEquals(SC_FORBIDDEN,response.statusCode());
            }catch (Exception e){
                Assert.fail("Can not connect to Elytron Http client");
            }
        });
    }
}
