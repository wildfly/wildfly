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
import org.jboss.as.cli.CommandContextFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleLinesCommandsTestCase {

    @Test
    public void testOperation() throws Exception {

        final StringBuilder buf = new StringBuilder();
        buf.append(":\\\n");
        buf.append("read-resource(\\\n");
        buf.append("include-defaults=true,\\\n");
        buf.append("recursive=false)");

        final CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.connectController();
            ctx.handle(buf.toString());
        } finally {
            ctx.terminateSession();
        }
    }

    @Test
    @Ignore("AS7-4734")
    public void testCommand() throws Exception {

        final StringBuilder buf = new StringBuilder();
        buf.append("read-attribute\\\n");
        buf.append("product-name\\\n");
        buf.append("--verbose");

        final CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.connectController();
            ctx.handle(buf.toString());
        } finally {
            ctx.terminateSession();
        }
    }
}
