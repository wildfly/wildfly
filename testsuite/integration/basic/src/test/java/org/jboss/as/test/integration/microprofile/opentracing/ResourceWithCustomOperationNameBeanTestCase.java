package org.jboss.as.test.integration.microprofile.opentracing;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.microprofile.opentracing.application.CustomOperationNameBean;
import org.jboss.as.test.integration.microprofile.opentracing.application.MockTracerFactory;
import org.jboss.as.test.integration.microprofile.opentracing.application.OpenTracingApplication;
import org.jboss.as.test.integration.microprofile.opentracing.application.WithCustomOperationNameEndpoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class ResourceWithCustomOperationNameBeanTestCase {
    @Inject
    Tracer tracer;

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(ResourceTracedTestCase.class);

        war.addClass(MockTracerFactory.class);
        war.addPackage(MockTracer.class.getPackage());
        war.addAsServiceProvider(TracerFactory.class, MockTracerFactory.class);

        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        war.addClass(OpenTracingApplication.class);
        war.addClass(WithCustomOperationNameEndpoint.class);
        war.addClass(CustomOperationNameBean.class);

        war.addClass(HttpRequest.class);

        return war;
    }

    @Test
    public void customOperationName() throws Exception {
        Assert.assertTrue(tracer instanceof MockTracer);
        MockTracer mockTracer = (MockTracer) tracer;

        performCall("opentracing/with-custom-operation-name");

        List<MockSpan> spans = mockTracer.finishedSpans();
        Assert.assertEquals(3, spans.size());

        Assert.assertEquals("my-custom-method-operation-name", spans.get(0).operationName());
        Assert.assertEquals("my-custom-class-operation-name", spans.get(1).operationName());
        Assert.assertTrue(spans.get(2).operationName().contains(WithCustomOperationNameEndpoint.class.getName()));

    }

    private void performCall(String path) throws Exception {
        HttpRequest.get(url + path, 10, TimeUnit.SECONDS);
    }
}
