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
package org.jboss.as.cli.handlers;


import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineCompleter;


/**
 *
 * @author Alexey Loubyansky
 */
public class VersionHandler implements CommandHandler {

    public static final VersionHandler INSTANCE = new VersionHandler();

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#isAvailable(org.jboss.as.cli.CommandContext)
     */
    @Override
    public boolean isAvailable(CommandContext ctx) {
        return true;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#getArgumentCompleter()
     */
    @Override
    public CommandLineCompleter getArgumentCompleter() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#isBatchMode()
     */
    @Override
    public boolean isBatchMode() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#handle(org.jboss.as.cli.CommandContext)
     */
    @Override
    public void handle(CommandContext ctx) {
        ctx.printLine("JBoss Admin Command-line Interface");
        ctx.printLine("JBOSS_HOME: " + SecurityActions.getEnvironmentVariable("JBOSS_HOME"));
        ctx.printLine("JAVA_HOME: " + SecurityActions.getEnvironmentVariable("JAVA_HOME"));
        ctx.printLine("java.version: " + SecurityActions.getSystemProperty("java.version"));
        ctx.printLine("java.vm.vendor: " + SecurityActions.getSystemProperty("java.vm.vendor"));
        ctx.printLine("java.vm.version: " + SecurityActions.getSystemProperty("java.vm.version"));
        ctx.printLine("os.name: " + SecurityActions.getSystemProperty("os.name"));
        ctx.printLine("os.version: " + SecurityActions.getSystemProperty("os.version"));
    }

    @Override
    public boolean hasArgument(String name) {
        return false;
    }

    @Override
    public boolean hasArgument(int index) {
        return false;
    }
}
