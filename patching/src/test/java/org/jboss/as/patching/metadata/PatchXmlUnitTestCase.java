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

package org.jboss.as.patching.metadata;

import static org.junit.Assert.assertEquals;
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
public class PatchXmlUnitTestCase {

    @Test
    public void testParse() throws Exception {

        final InputStream is = getResource("test01.xml");
        final Patch patch = PatchXml.parse(is);
        // Patch
        assertNotNull(patch);
        assertNotNull(patch.getPatchId());
        assertNotNull(patch.getDescription());
        assertNotNull(patch.getPatchType());
        assertEquals(Patch.PatchType.CUMULATIVE, patch.getPatchType());
        assertNotNull(patch.getResultingVersion());
        assertNotNull(patch.getAppliesTo().get(0));
    }

    @Test
    public void testMarshall() throws Exception {
        final InputStream is = getResource("test01.xml");
        final String original = toString(is);
        final Patch patch = PatchXml.parse(is);

        final StringWriter writer = new StringWriter();
        PatchXml.marshal(writer, patch);
        final String marshalled = writer.toString();

        XMLUtils.compareXml(original, marshalled, false);
    }

    static InputStream getResource(String name) throws IOException {
        final URL resource = PatchXmlUnitTestCase.class.getClassLoader().getResource(name);
        assertNotNull(name, resource);
        return resource.openStream();
    }

    private String toString(InputStream is) {
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
