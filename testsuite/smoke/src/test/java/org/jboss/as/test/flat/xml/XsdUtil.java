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

package org.jboss.as.test.flat.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Date: 21.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class XsdUtil {

    /**
     * Don't construct
     */
    private XsdUtil() {
    }


    static URL discover(final String xsdName) {
        URL url = null;
        try {
            // user.dir will point to the root of this module
            final File modDir = new File(System.getProperty("user.dir"));
            File baseDir = new File(modDir, "../../build/target/");
            final File[] children = baseDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.getName().startsWith("jboss-")) {
                        baseDir = new File(child, "docs/schema");
                        break;
                    }
                }
            }
            final File file = new File(baseDir, xsdName);
            if (file.exists())
                url = file.toURI().toURL();
        } catch (IOException e) {
            url = null;
        }
        // Search
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (url == null)
            url = classLoader.getResource("docs/schema" + xsdName);
        if (url == null)
            url = classLoader.getResource("docs/" + xsdName);
        if (url == null)
            url = classLoader.getResource("schema/" + xsdName);
        if (url == null)
            url = classLoader.getResource(xsdName);
        return url;
    }
}
