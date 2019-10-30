/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.concurrent;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

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
