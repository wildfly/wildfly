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

package org.jboss.as.test.integration.management.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexey Loubyansky
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StringValuesParsedAsNonObjectsTestCase {

    private static final String VALUE = "value";
    private CommandContext ctx;

    @Before
    public void setup() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();
        ctx.handle("command add --node-type=system-property --command-name=system-property");
    }

    @After
    public void tearDown() throws Exception {
        ctx.disconnectController();
    }

    @Test
    public void testEqualsSignInParameter() throws Exception {
        // if the string is parsed with the default general parser
        // the returned value will be simply 'b'
        assertEquals("a=b", parsedAddCommandValue("a=b"));
    }

    @Test
    public void testEqualsSignInProperty() throws Exception {
        // if the string is parsed with the default general parser
        // the returned value will be simply 'b'
        assertEquals("a=b", parsedSetPropertyCommandValue("a=b"));
    }

    private String parsedAddCommandValue(String input) throws Exception {
        final ModelNode req = ctx.buildRequest("system-property add --name=test --value=" + input);
        ModelNode valueNode = req.get(VALUE);
        assertTrue(valueNode.isDefined());
        return valueNode.asString();
    }

    private String parsedSetPropertyCommandValue(String input) throws Exception {
        final ModelNode req = ctx.buildRequest("system-property --name=test --value=" + input);
        final ModelNode steps = req.get("steps");
        assertTrue(steps.isDefined());
        final ModelNode wa = steps.asList().get(0);
        assertTrue(wa.isDefined());
        ModelNode valueNode = wa.get(VALUE);
        assertTrue(valueNode.isDefined());
        return valueNode.asString();
    }
}
