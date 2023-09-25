/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet3;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

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
