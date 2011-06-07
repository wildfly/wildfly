/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded.ejb3;

import org.junit.Test;

import java.io.File;
import java.util.Collections;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ClassPathEjbJarScannerTest {
    @Test
    public void testEmpty() {
        // use surefire.test.class.path, because we don't (want to) know our test environment
        final String oldProperty = System.setProperty("surefire.test.class.path", "non-existent");
        // setup an empty entry in the middle
        System.setProperty("surefire.test.class.path", "/dummy1" + File.pathSeparator + File.pathSeparator + "/dummy2");
        try {
            ClassPathEjbJarScanner.getEjbJars(Collections.<Object, Object>emptyMap());
        }
        finally {
            if (oldProperty != null)
                System.setProperty("surefire.test.class.path", oldProperty);
        }
    }

    @Test
    public void testNonExistent() {
        // use surefire.test.class.path, because we don't (want to) know our test environment
        final String oldProperty = System.setProperty("surefire.test.class.path", "non-existent");
        try {
            ClassPathEjbJarScanner.getEjbJars(Collections.<Object, Object>emptyMap());
        }
        finally {
            if (oldProperty != null)
                System.setProperty("surefire.test.class.path", oldProperty);
        }
    }

}
