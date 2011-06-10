/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.interceptor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.testsuite.integration.osgi.OSGiTestSupport;
import org.jboss.as.testsuite.integration.osgi.interceptor.bundle.EndpointServlet;
import org.jboss.as.testsuite.integration.osgi.interceptor.bundle.HttpMetadata;
import org.jboss.as.testsuite.integration.osgi.interceptor.bundle.InterceptorActivator;
import org.jboss.as.testsuite.integration.osgi.interceptor.bundle.ParserInterceptor;
import org.jboss.as.testsuite.integration.osgi.interceptor.bundle.PublisherInterceptor;
import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.http.HttpServiceCapability;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.startlevel.StartLevel;

/**
 * A test that deployes a bundle that contains some metadata and an interceptor bundle that processes the metadata and
 * registeres an http endpoint from it.
 *
 * The idea is that the bundle does not process its own metadata. Instead this work is delegated to some specialized metadata
 * processor (i.e. the interceptor)
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Oct-2009
 */
@RunWith(Arquillian.class)
public class LifecycleInterceptorTestCase extends OSGiTestSupport {

    static final String PROCESSOR_NAME = "interceptor-processor";
    static final String ENDPOINT_NAME = "interceptor-endpoint";

    @Inject
    public BundleContext context;

    @ArquillianResource
    public Deployer deployer;

    @Deployment
    @StartLevelAware(startLevel = 3)
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-interceptor");
        archive.addClass(OSGiTestSupport.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(StartLevel.class, HttpServiceCapability.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testServletAccess() throws Exception {

        changeStartLevel(context, 3, 10, TimeUnit.SECONDS);

        deployer.deploy(PROCESSOR_NAME);
        try {
            deployer.deploy(ENDPOINT_NAME);
            try {
                String line = getHttpResponse("/example-interceptor/servlet", 5000);
                assertEquals("Hello from Servlet", line);
            } finally {
                deployer.undeploy(ENDPOINT_NAME);
            }
        } finally {
            deployer.undeploy(PROCESSOR_NAME);
        }
    }

    @Deployment(name = PROCESSOR_NAME, managed = false, testable = false)
    public static JavaArchive getProcessorArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, PROCESSOR_NAME);
        archive.addClasses(HttpMetadata.class, InterceptorActivator.class, ParserInterceptor.class, PublisherInterceptor.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(InterceptorActivator.class);
                builder.addImportPackages(BundleActivator.class, LifecycleInterceptor.class, HttpService.class);
                builder.addImportPackages(HttpServlet.class, Servlet.class, Logger.class, VirtualFile.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = ENDPOINT_NAME, managed = false, testable = false)
    public static JavaArchive getEndpointArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, ENDPOINT_NAME);
        archive.addClasses(EndpointServlet.class);
        archive.addAsResource("osgi/interceptor/http-metadata.properties", "http-metadata.properties");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(HttpServlet.class, Servlet.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    private String getHttpResponse(String reqPath, int timeout) throws IOException {
        return HttpServiceCapability.getHttpResponse("localhost", 8090, reqPath, timeout);
    }
}