/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.launch;

import java.util.HashMap;

import junit.framework.Assert;

import org.jboss.as.osgi.launcher.EmbeddedOSGiFrameworkLauncher;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * @author David Bosschaert
 */
public class EmbeddedOSGiFrameworkLauncherTestCase extends EmbeddedOSGiFrameworkLauncherTestBase {
    @Test
    public void testLaunchFramework() throws Exception {
        EmbeddedOSGiFrameworkLauncher launcher = new EmbeddedOSGiFrameworkLauncher();
        launcher.configure(new HashMap<String, String>());
        try {
            launcher.start();
            BundleContext systemBundleContext = (BundleContext) launcher.activateOSGiFramework();
            Assert.assertEquals(0L, systemBundleContext.getBundle().getBundleId());
        } finally {
            launcher.stop();
        }
    }
}
