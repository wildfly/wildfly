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
package org.jboss.as.test.smoke.ee;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks that EE8 preview APIs and implementations are not included in default configuration
 * This is valid test case till full EE8 compliance is achieved, after that this test case can be removed.
 *
 * @author Rostislav Svoboda
 */
@RunWith(Arquillian.class)
public class NoEE8InDefaultConfigurationTestCase {
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "no-ee8-in-default-config.jar");
        archive.addClass(NoEE8InDefaultConfigurationTestCase.class);
        return archive; // No need to explicitly define api dependencies in MANIFEST.MF
    }

    @Test
    public void unavailableJAXRS21() throws Exception {
        String fqcn = "javax.ws.rs.sse.Sse";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));
    }

    @Test
    public void ensureJAXRS20() throws Exception {
        String fqcn = "javax.ws.rs.client.ClientBuilder";
        assertTrue("Class " + fqcn +" is not available", isClassAvailable(fqcn));
    }

    @Test
    public void unavailableJSONP11() throws Exception {
        String fqcn = "javax.json.stream.JsonCollectors";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));
    }

    @Test
    public void ensureJSONP10() throws Exception {
        String fqcn = "javax.json.stream.JsonParser";
        assertTrue("Class " + fqcn +" is not available", isClassAvailable(fqcn));
    }

    @Test
    public void unavailableJSONB10() throws Exception {
        String fqcn = "javax.json.bind.JsonbBuilder";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));
    }

    @Test
    public void unavailableCDI20() throws Exception {
        String fqcn = "javax.enterprise.inject.spi.InterceptionFactory";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));
    }

    @Test
    public void unavailableServlet40() throws Exception {
        String fqcn = "javax.servlet.GenericFilter";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));

        fqcn = "javax.servlet.http.HttpServletMapping";
        assertFalse("Class " + fqcn +" is available", isClassAvailable(fqcn));
    }

    private boolean isClassAvailable(String s) {
        boolean classAvailable = false;
        try {
            Class<?> sse = Class.forName(s);
            classAvailable = true;
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return classAvailable;
    }
}
