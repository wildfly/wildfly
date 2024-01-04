/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Martin Kouba
 * @author Matus Abaffy
 */
@RunAsClient
@RunWith(Arquillian.class)
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
