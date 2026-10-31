/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Martin Kouba
 * @author Matus Abaffy
 */
@RunAsClient
@ExtendWith(ArquillianExtension.class)
public class ServletInjectionSupportTestCase extends InjectionSupportTestCase {

    @Deployment
    public static WebArchive createTestArchive() {
        return createTestArchiveBase().addClass(TestServlet.class).addClasses(constructTestsHelperClasses);
    }

    @Test
    public void testFieldInjection() throws IOException, ExecutionException, TimeoutException {
        assertNotNull(doGetRequest("/TestServlet?mode=field"));
    }

    @Test
    public void testSetterInjection() throws IOException, ExecutionException, TimeoutException {
        assertNotNull(doGetRequest("/TestServlet?mode=method"));
    }

    @Test
    public void testConstructorInjection() throws IOException, ExecutionException, TimeoutException {
        doGetRequest("/TestServlet?mode=constructor");
    }

    @Test
    public void testAroundInvokeInterceptor() throws IOException, ExecutionException, TimeoutException {
        // Servlet.service(ServletRequest, ServletResponse) must be intercepted
        assertEquals("0", doGetRequest("/TestServlet?mode=interceptorReset"));
        assertEquals("1", doGetRequest("/TestServlet?mode=aroundInvokeVerify"));
    }

    @Test
    public void testAroundConstructInterceptor() throws IOException, ExecutionException, TimeoutException {
        assertEquals("AroundConstructInterceptor#Joe#TestServlet", doGetRequest("/TestServlet?mode=aroundConstructVerify"));
    }
}
