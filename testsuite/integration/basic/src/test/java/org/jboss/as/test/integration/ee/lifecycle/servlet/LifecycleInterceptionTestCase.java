/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author Matus Abaffy
 */
public abstract class LifecycleInterceptionTestCase {

    protected static final String REMOTE = "remote";

    private static final String BEANS_XML = "beans.xml";

    protected static WebArchive createRemoteTestArchiveBase() {
        return ShrinkWrap.create(WebArchive.class, "remote.war")
                .addClasses(LifecycleCallbackBinding.class, LifecycleCallbackInterceptor.class, InfoClient.class,
                        InitServlet.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, BEANS_XML)
                // InfoClient requires SocketPermission
                .addAsManifestResource(
                        createPermissionsXmlAsset(new SocketPermission(TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")),
                        "permissions.xml");
    }

    protected static WebArchive createMainTestArchiveBase() {
        return ShrinkWrap.create(WebArchive.class, "main.war")
                .addClass(InfoServlet.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, BEANS_XML);
    }

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource//(InfoServlet.class)
    URL infoContextPath;

    protected String doGetRequest(String path) throws IOException, ExecutionException, TimeoutException {
        return HttpRequest.get(path, 10, TimeUnit.SECONDS);
    }
}
