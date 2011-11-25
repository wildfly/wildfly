/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.osgi.logging.bundle.LoggingActivator;
import org.jboss.as.test.integration.osgi.logging.bundle.LoggingDelegate;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

import javax.inject.Inject;
import java.io.InputStream;

/**
 * @author David Bosschaert
 */
@RunWith(Arquillian.class)
public class LoggingTestCase {
    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-logging");
        archive.addClasses(LoggingActivator.class, LoggingDelegate.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(LoggingActivator.class);
                builder.addImportPackages(org.jboss.logging.Logger.class);
                builder.addImportPackages(org.apache.commons.logging.LogFactory.class);
                builder.addImportPackages(org.slf4j.LoggerFactory.class);
                builder.addImportPackages(org.apache.log4j.Logger.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testLogging() throws Exception {
        bundle.start();

        // If the test fails the bundle will throw an exception during start
        OSGiTestHelper.assertBundleState(Bundle.ACTIVE, bundle.getState());
    }
}
