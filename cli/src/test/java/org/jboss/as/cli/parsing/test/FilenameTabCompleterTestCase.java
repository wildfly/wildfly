/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.io.File;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.impl.FileSystemPathArgument;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexey Loubyansky
 *
 */
public class FilenameTabCompleterTestCase {

    private CommandContext ctx;
    private FileSystemPathArgument arg;
    private DefaultCallbackHandler parsedCmd;
    
    @Before
    public void setup() throws CliInitializationException {
        ctx = CommandContextFactory.getInstance().newCommandContext();
        final DefaultFilenameTabCompleter completer = new DefaultFilenameTabCompleter(ctx);

        final CommandHandlerWithArguments cmd = new CommandHandlerWithArguments(){

            @Override
            public boolean isAvailable(CommandContext ctx) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isBatchMode(CommandContext ctx) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void handle(CommandContext ctx) throws CommandLineException {
                // TODO Auto-generated method stub
                
            }};
        
        arg = new FileSystemPathArgument(cmd, completer, 0, "arg");
        parsedCmd = new DefaultCallbackHandler();
    }
    
    @After
    public void tearDown() {
        ctx.terminateSession();
        ctx = null;
        arg = null;
        parsedCmd = null;
    }
    
    @Test
    public void testTranslateGetValue() throws Exception {
        parsedCmd.parse(null, "cmd ~" + File.separator, ctx);
        assertEquals(SecurityActions.getProperty("user.home") + File.separator, arg.getValue(parsedCmd));
    }

    @Test
    public void testTranslateGetValueRequired() throws Exception {
        parsedCmd.parse(null, "cmd ~" + File.separator, ctx);
        assertEquals(SecurityActions.getProperty("user.home") + File.separator, arg.getValue(parsedCmd, true));
    }
}
