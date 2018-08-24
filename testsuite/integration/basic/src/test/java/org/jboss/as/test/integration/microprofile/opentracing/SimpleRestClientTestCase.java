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
