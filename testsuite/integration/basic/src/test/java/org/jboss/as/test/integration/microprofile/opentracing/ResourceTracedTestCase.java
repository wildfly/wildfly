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

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
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
import javax.servlet.ServletContext;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class ResourceTracedTestCase {
    @Inject
    Tracer tracer;

    @ArquillianResource
    private URL url;

    @Inject
    ServletContext servletContext;

    /**
     * Permissions required in case security manager is enabled.
     */
    private static final String PERMISSIONS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<permissions version=\"7\">\n"
            + "    <permission>\n"
            + "        <class-name>java.lang.RuntimePermission</class-name>\n"
            + "        <name>modifyThread</name>\n"
            + "    </permission>\n"
            + "    <permission>\n"
            + "        <class-name>java.net.SocketPermission</class-name>\n"
            + "        <name>*</name>\n"
            + "        <actions>connect,resolve</actions>\n"
            + "    </permission>\n"
            + "</permissions>";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(ResourceTracedTestCase.class);

        war.addClass(MockTracerFactory.class);
        war.addPackage(MockTracer.class.getPackage());
        war.addAsServiceProvider(TracerFactory.class, MockTracerFactory.class);

        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        war.addClass(OpenTracingApplication.class);
        war.addClass(TracedEndpoint.class);

        war.addClass(HttpRequest.class);

        war.addAsManifestResource(new StringAsset(PERMISSIONS), "permissions.xml");

        return war;
    }

    @Test
    public void tracedEndpointYieldsSpan() throws Exception {
        Assert.assertTrue(tracer instanceof MockTracer);
        MockTracer mockTracer = (MockTracer) tracer;

        performCall("opentracing/traced");

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals(
                (servletContext.getContextPath() + ".war").substring(1),
                servletContext.getInitParameter("smallrye.opentracing.serviceName")
        );
    }

    private void performCall(String path) throws Exception {
        HttpRequest.get(url + path, 10, TimeUnit.SECONDS);
    }
}
