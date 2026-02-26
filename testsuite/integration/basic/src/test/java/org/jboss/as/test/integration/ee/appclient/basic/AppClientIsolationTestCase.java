/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class AppClientIsolationTestCase extends AbstractSimpleApplicationClientTestCase {

    private static final String APP_NAME = AppClientIsolationTestCase.class.getSimpleName();
    private static final String WAR_NAME = APP_NAME + ".war";

    private static EnterpriseArchive archive;


    @Deployment(testable = false)
    public static Archive<?> deploy() {

        // Build the usual ear, but with the AppClientSingletonRemote interface duplicated
        final EnterpriseArchive ear = buildAppclientEar(APP_NAME, false);

        WebArchive war =  ShrinkWrap.create(WebArchive.class, WAR_NAME);
        war.addClasses(Servlet.class);

        ear.addAsModule(war);

        archive = ear;
        return ear;
    }

    @ArquillianResource
    private URL url;

    public AppClientIsolationTestCase() {
        super(APP_NAME);
    }

    @Override
    public Archive<?> getArchive() {
        return AppClientIsolationTestCase.archive;
    }

    // Do the appclient invocation to prove that not sharing the EJB interface didn't break that
    @Test
    public void simpleAppClientTest() throws Exception {
        super.simpleAppClientTest();
    }

    @Test
    public void appclientIsolationTest() throws Exception {
        // Invoke on the servlet to confirm it doesn't have problems due to the visibility of the appclient jar's duplicate class
        String result = HttpRequest.get(url.toExternalForm() + "/servlet", 1000, SECONDS);
        assertEquals("OK", result);
    }
}
