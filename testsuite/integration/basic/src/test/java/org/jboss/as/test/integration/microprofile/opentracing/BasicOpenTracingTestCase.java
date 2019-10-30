package org.jboss.as.test.integration.microprofile.opentracing;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Tracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
public class BasicOpenTracingTestCase {

    @Inject
    Tracer tracer;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(BasicOpenTracingTestCase.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void hasDefaultInjectedTracer() {
        Assert.assertNotNull(tracer);
        Assert.assertTrue(tracer instanceof JaegerTracer);
    }
}
