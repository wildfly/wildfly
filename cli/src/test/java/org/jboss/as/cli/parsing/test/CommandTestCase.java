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
package org.jboss.as.cli.parsing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.impl.DefaultParsedCommand;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandTestCase {

    @Test
    public void testCommandOnly() throws Exception {

        DefaultParsedCommand cmd = parse("some-command");
        assertEquals("some-command", cmd.getCommandName());
        assertFalse(cmd.hasArguments());
        assertNull(cmd.getOutputTarget());
    }

    @Test
    public void testCommandWithArgsAndOutputTarget() throws Exception {

        DefaultParsedCommand cmd = parse(" some-command --name=value --name1 value1 > command.log");
        assertEquals("some-command", cmd.getCommandName());
        assertTrue(cmd.hasArguments());
        assertTrue(cmd.hasArgument("--name"));
        assertEquals("value", cmd.getArgument("--name"));
        assertTrue(cmd.hasArgument("--name1"));
        assertNull(cmd.getArgument("--name1"));

        List<String> otherArgs = cmd.getOtherArguments();
        assertEquals(1, otherArgs.size());
        assertEquals("value1", otherArgs.get(0));

        assertEquals("command.log", cmd.getOutputTarget());
    }

    protected DefaultParsedCommand parse(String line) {
        DefaultParsedCommand args = new DefaultParsedCommand();
        try {
            args.parse(line);
        } catch (CommandFormatException e) {
            org.junit.Assert.fail(e.getLocalizedMessage());
        }
        return args;
    }
}
