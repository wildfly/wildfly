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
package org.jboss.as.test.integration.management.cli;


import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CommandSubstitutionTestCase {

    private static final String ADD = "add";
    private static final String NAME = "name";
    private static final String NODE = "node";
    private static final String READ_ATTRIBUTE = "read-attribute";
    private static final String SYSTEM_PROPERTY = "system-property";
    private static final String TEST = "prop_test";
    private static final String VALUE = "value";
    
    private CommandContext ctx;
    
    @Before
    public void setup() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();
        setProperty(ADD, ADD);
        setProperty(NAME, NAME);
        setProperty(NODE, NODE);
        setProperty(READ_ATTRIBUTE, READ_ATTRIBUTE);
        setProperty(SYSTEM_PROPERTY, SYSTEM_PROPERTY);
        setProperty(VALUE, VALUE);
    }
    
    @After
    public void tearDown() throws Exception {
        try {
            safeRemove(ADD);
            safeRemove(NAME);
            safeRemove(NODE);
            safeRemove(READ_ATTRIBUTE);
            safeRemove(SYSTEM_PROPERTY);
            safeRemove(TEST);
            safeRemove(VALUE);
            ctx.setVariable("a", null);
            ctx.setVariable("b", null);
        } finally {
            ctx.terminateSession();
        }
    }
    
    @Test
    public void testOperation() throws Exception {
        final StringBuilder buf = new StringBuilder();
        buf.append('/')
           .append(opSubstitution(SYSTEM_PROPERTY)).append('=').append(TEST).append(':')
           .append(cmdSubstitution(ADD)).append('(').append(opSubstitution(VALUE)).append('=').append("1").append(')');
        ctx.handle(buf.toString());
        assertEquals("1", readProperty(TEST));
        
        buf.setLength(0);
        buf.append(opSubstitution(READ_ATTRIBUTE))
           .append(" --").append(cmdSubstitution(NODE)).append('=')
           .append(opSubstitution(SYSTEM_PROPERTY)).append('=').append(TEST).append(' ')
           .append(cmdSubstitution(VALUE));
        assertEquals("1", executeReadProperty(buf.toString()));
    }

    @Test
    public void setVariableValues() throws Exception {
        assertNull(ctx.getVariable("a"));
        assertNull(ctx.getVariable("b"));
        ctx.handle("set a=" + opSubstitution(ADD) + " b=" + cmdSubstitution(NAME));
        assertEquals(ADD, ctx.getVariable("a"));
        assertEquals(NAME, ctx.getVariable("b"));
    }
    
    private String opSubstitution(String prop) {
        return "`/system-property=" + prop + ":read-attribute(name=value)`";
    }

    private String cmdSubstitution(String prop) {
        return "`read-attribute --node=system-property=" + prop + " value`";
    }

    protected void setProperty(String prop, String value) throws Exception {
        ctx.handle("/system-property=" + prop + ":add(value=" + value + ")");
    }

    protected void safeRemove(String prop) {
        ctx.handleSafe("/system-property=" + prop + ":remove");
    }

    protected String readProperty(final String prop) throws Exception {
        String operation = "/system-property=" + prop + ":read-attribute(name=value)";
        return executeReadProperty(operation);
    }

    private String executeReadProperty(String operation) throws Exception {
        final ModelNode request = ctx.buildRequest(operation);
        final ModelControllerClient client = ctx.getModelControllerClient();
        final ModelNode response = client.execute(request);
        return response.get("result").asString();
    }
}
