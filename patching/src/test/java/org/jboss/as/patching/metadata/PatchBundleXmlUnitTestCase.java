/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.metadata;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchBundleXmlUnitTestCase {

    @Test
    public void testBasic() throws Exception {

        final InputStream is = getResource("multi-patch-01.xml");
        final BundledPatch bundledPatch = PatchBundleXml.parse(is);
        Assert.assertNotNull(bundledPatch);
        Assert.assertFalse(bundledPatch.getPatches().isEmpty());

    }

    @Test
    public void testMarshal() throws Exception {
        doMarshall("multi-patch-01.xml");
    }

    static void doMarshall(final String fileName) throws Exception {
        final String original = toString(fileName);

        final InputStream is = getResource(fileName);
        final BundledPatch patch = PatchBundleXml.parse(is);

        final StringWriter writer = new StringWriter();
        PatchBundleXml.marshal(writer, patch);
        final String marshalled = writer.toString();

        //System.out.println(original);
        //System.out.println(marshalled);

        XMLUtils.compareXml(original, marshalled, false);
    }

    static InputStream getResource(final String name) throws IOException {
        final URL resource = PatchBundleXmlUnitTestCase.class.getClassLoader().getResource(name);
        assertNotNull(name, resource);
        return resource.openStream();
    }

    static String toString(final String fileName) throws Exception {
        final InputStream is = getResource(fileName);
        assertNotNull(is);
        try {
            is.mark(0);
            String out = new Scanner(is).useDelimiter("\\A").next();
            is.reset();
            return out;
        } catch (Exception e) {
            return "";
        }
    }

}
