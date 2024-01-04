/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.concurrent;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultContextServiceServletTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(HttpRequest.class, DefaultContextServiceTestServlet.class, TestServletRunnable.class);
        war.addAsManifestResource(
                createPermissionsXmlAsset(
                        // Needed for getting the principle and logging in in the DefaultContextServiceTestServlet
                        new RuntimePermission("org.jboss.security.*"),
                        // TODO (jrp) This permission needs to be removed once WFLY-4176 is resolved
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("modifyThread"),
                        new RuntimePermission("getBootModuleLoader")
                        ), "permissions.xml");
        return war;
    }

    @Test
    public void testServlet() throws Exception {
        HttpRequest.get(url.toExternalForm() + "simple", 10, SECONDS);
    }

}
