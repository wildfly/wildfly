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
package io.smallrye.opentracing.arquillian;

import java.io.File;

import javax.ws.rs.ext.Providers;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.mock.MockTracer;
import io.smallrye.opentracing.ExceptionMapper;
import io.smallrye.opentracing.MockTracerResolver;
import io.smallrye.opentracing.ResteasyClientTracingRegistrarProvider;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrar;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;

/**
 * @author Pavol Loffay
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addClass(ExceptionMapper.class);
            extensionsJar.addAsServiceProvider(Providers.class, ExceptionMapper.class);

            extensionsJar.addClass(ResteasyClientTracingRegistrarProvider.class);
            extensionsJar.addClass(ClientTracingRegistrarProvider.class);
            extensionsJar.addClass(ClientTracingRegistrar.class);
            extensionsJar.addAsServiceProvider(ClientTracingRegistrarProvider.class,
                    ResteasyClientTracingRegistrarProvider.class);

            extensionsJar.addClasses(MockTracerResolver.class);
            extensionsJar.addPackage(MockTracer.class.getPackage());
            extensionsJar.addAsServiceProvider(TracerResolver.class, MockTracerResolver.class);

            WebArchive war = WebArchive.class.cast(archive);
            war.addAsLibraries(extensionsJar);

            final File archiveDir = new File("target/archives");
            archiveDir.mkdirs();
            File moduleFile = new File(archiveDir, "testapp.war");
            war.as(ZipExporter.class).exportTo(moduleFile, true);
        }
    }
}
