/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

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
