/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.servlet3;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test the programmatic login feature of Servlet 3
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityProgrammaticLoginTestCase extends WebSecurityPasswordBasedBase {

    private static final String warSuffix = ".war";
    private static final String warName = "web-secure-programmatic-login";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warName + warSuffix);
        war.addAsWebInfResource(WebSecurityProgrammaticLoginTestCase.class.getPackage(), "jboss-web.xml", "jboss-web" +
                ".xml");
        war.addClass(LoginServlet.class);
        war.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("org.jboss.security.*")),
                "permissions.xml");

        return war;
    }

    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse res = httpclient.execute(new HttpGet(managementClient.getWebUri() + "/" + getContextPath() +
                    "/login/?username=" + user + "&password=" + pass));
            Assert.assertEquals(expectedStatusCode, res.getStatusLine().getStatusCode());
        }
    }

    public String getContextPath() {
        return warName;
    }
}
