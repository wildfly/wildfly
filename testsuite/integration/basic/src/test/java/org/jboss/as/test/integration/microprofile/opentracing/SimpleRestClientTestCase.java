/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.microprofile.opentracing;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrar;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.microprofile.opentracing.application.MockTracerFactory;
import org.jboss.as.test.integration.microprofile.opentracing.application.OpenTracingApplication;
import org.jboss.as.test.integration.microprofile.opentracing.application.TracedEndpoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.List;

@RunWith(Arquillian.class)
public class SimpleRestClientTestCase {
    @Inject
    Tracer tracer;

    @ArquillianResource
    URL url;

    /**
     * Permissions required in case security manager is enabled.
     * Note: I was not able to properly determine what exact permission is required,
     * thus allowed all of them... should not be problem for the test, though.
     */
    private static final String PERMISSIONS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<permissions version=\"7\">\n"
            + "    <permission>\n"
            + "        <class-name>java.security.AllPermission</class-name>\n"
            + "        <name>*</name>\n"
            + "    </permission>\n"
            + "</permissions>";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(SimpleRestClientTestCase.class);

        war.addClass(OpenTracingApplication.class);
        war.addClass(TracedEndpoint.class);
        war.addClass(MockTracerFactory.class);

        war.addPackage(MockTracer.class.getPackage());
        war.addAsServiceProvider(TracerFactory.class, MockTracerFactory.class);

        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        war.addAsManifestResource(new StringAsset(PERMISSIONS), "permissions.xml");

        return war;
    }

    @Test
    public void clientRequestSpanJoinsServer() {
        // sanity checks
        Assert.assertNotNull(tracer);
        Assert.assertTrue(tracer instanceof MockTracer);

        // test
        // the first span
        try (Scope ignored = tracer.buildSpan("existing-span").startActive(true)) {

            // the second span is the client request, as a child of `existing-span`
            Client restClient = ClientTracingRegistrar.configure(ClientBuilder.newBuilder()).build();

            // the third span is the traced endpoint, child of the client request
            String targetUrl = url.toString() + "opentracing/traced";
            WebTarget target = restClient.target(targetUrl);

            try (Response response = target.request().get()) {
                // just a sanity check
                Assert.assertEquals(200, response.getStatus());
            }
        }

        // verify
        MockTracer mockTracer = (MockTracer) tracer;
        List<MockSpan> spans = mockTracer.finishedSpans();
        Assert.assertEquals(3, spans.size());
        long traceId = spans.get(0).context().traceId();
        for (MockSpan span : spans) {
            // they should all belong to the same trace
            Assert.assertEquals(traceId, span.context().traceId());
        }
    }

}
