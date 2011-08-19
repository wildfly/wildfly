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
package org.jboss.as.cli.completion.address.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.as.cli.completion.mock.MockNode;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class NodeNameFormattingTestCase extends AbstractAddressCompleterTest {

    public NodeNameFormattingTestCase() {
        super();

        MockNode root = addRoot("root");
        root.addChild("colon:");
        root.addChild("slash/");
        root.addChild("back\\slash");
        root.addChild("both/:");
        root.addChild("all equals= slash/ colon: \"quotes\"");
        root.addChild("equals=equals");

        MockNode datasource = root.addChild("datasources").addChild("data-source");
        datasource.addChild("java:/H2DS1");
        datasource.addChild("java:/H2DS2");
    }

    @Test
    public void testAll() {
        assertEquals(Arrays.asList("all equals= slash/ colon: \"quotes\"", "back\\slash", "both/:", "colon:", "datasources", "equals=equals", "slash/"), fetchCandidates("/root="));
    }

    @Test
    public void testColon() {
        //assertEquals(Arrays.asList("\"colon:\""), fetchCandidates("/root=c"));
        assertEquals(Arrays.asList("colon\\:"), fetchCandidates("/root=c"));
    }

    @Test
    public void testSlash() {
        //assertEquals(Arrays.asList("\"slash/\""), fetchCandidates("/root=s"));
        assertEquals(Arrays.asList("slash\\/"), fetchCandidates("/root=s"));
    }

    @Test
    public void testBackSlash() {
        assertEquals(Arrays.asList("back\\\\slash"), fetchCandidates("/root=ba"));
    }

    @Test
    public void testBoth() {
        //assertEquals(Arrays.asList("\"both/:\""), fetchCandidates("/root=b"));
        assertEquals(Arrays.asList("both\\/\\:"), fetchCandidates("/root=bo"));
    }

    @Test
    public void testQuotes() {
        //assertEquals(Arrays.asList("\"all equals= slash/ colon: \\\"quotes\\\"\""), fetchCandidates("/root=a"));
        assertEquals(Arrays.asList("all\\ equals\\=\\ slash\\/\\ colon\\:\\ \\\"quotes\\\""), fetchCandidates("/root=a"));
    }

    @Test
    public void testEquals() {
        assertEquals(Collections.emptyList(), fetchCandidates("/root=equals="));
        assertEquals(Arrays.asList("equals\\=equals"), fetchCandidates("/root=\"equals=\""));
    }

    @Test
    public void testH2DS() {
        assertEquals(Arrays.asList("data-source="), fetchCandidates("/root=datasources/"));
        assertEquals(Arrays.asList("java\\:\\/H2DS1", "java\\:\\/H2DS2"), fetchCandidates("/root=datasources/data-source=j"));
    }

}
