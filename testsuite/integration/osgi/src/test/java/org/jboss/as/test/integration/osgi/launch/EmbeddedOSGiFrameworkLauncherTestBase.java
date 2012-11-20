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

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

/**
 * @author David Bosschaert
 */
public abstract class EmbeddedOSGiFrameworkLauncherTestBase {
    private Properties savedProps;

    @Before
    public void setUp() throws Exception {
        URL classURL = getClass().getClassLoader().getResource(getClass().getName().replace('.', File.separatorChar) + ".class");
        String classStr = new File(classURL.toURI()).getAbsolutePath();
        String projectStr = classStr.substring(0, classStr.lastIndexOf("target"));
        File projectDir = new File(projectStr);
        File baseDir = projectDir.getParentFile().getParentFile().getParentFile();
        File buildDir = new File(baseDir, "build/target");
        File jbossDir = null;
        for (File f : buildDir.listFiles()) {
            if (f.getName().startsWith("jboss-as-")) {
                jbossDir = f;
                break;
            }
        }

        // This can only be run once the build dir is there
        if (jbossDir == null)
            throw new IllegalStateException("Unable to find JBoss Build Dir, make sure that the ");

        savedProps = new Properties();
        savedProps.putAll(System.getProperties());
        System.setProperty("jboss.home", jbossDir.getAbsolutePath());
    }

    @After
    public void tearDown() {
        System.setProperties(savedProps);
    }
}