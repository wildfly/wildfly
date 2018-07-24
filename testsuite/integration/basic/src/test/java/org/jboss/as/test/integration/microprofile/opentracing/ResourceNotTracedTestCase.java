package org.jboss.as.test.integration.microprofile.opentracing;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.microprofile.opentracing.application.MockTracerFactory;
import org.jboss.as.test.integration.microprofile.opentracing.application.NotTracedEndpoint;
import org.jboss.as.test.integration.microprofile.opentracing.application.OpenTracingApplication;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class ResourceNotTracedTestCase {
    @Inject
    Tracer tracer;

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(ResourceNotTracedTestCase.class);

        war.addClass(MockTracerFactory.class);
        war.addPackage(MockTracer.class.getPackage());
        war.addAsServiceProvider(TracerFactory.class, MockTracerFactory.class);

        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        war.addClass(OpenTracingApplication.class);
        war.addClass(NotTracedEndpoint.class);

        war.addClass(HttpRequest.class);

        return war;
    }

    @Test
    public void notTracedEndpointYieldsNoSpans() throws Exception {
        Assert.assertTrue(tracer instanceof MockTracer);
        MockTracer mockTracer = (MockTracer) tracer;

        performCall("opentracing/not-traced");

        Assert.assertEquals(0, mockTracer.finishedSpans().size());
    }

    private void performCall(String path) throws Exception {
        HttpRequest.get(url + path, 10, TimeUnit.SECONDS);
    }

}
