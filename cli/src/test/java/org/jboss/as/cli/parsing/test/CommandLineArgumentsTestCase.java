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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.impl.DefaultParsedArguments;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandLineArgumentsTestCase {

    @Test
    public void testDefault() throws Exception {

        ParsedArguments args = parse("../../../../testsuite/smoke/target/deployments/test-deployment.sar --name=my.sar --disabled --runtime-name=myrt.sar --force");
        assertTrue(args.hasArguments());
        assertTrue(args.hasArgument("--name"));
        assertTrue(args.hasArgument("--runtime-name"));
        assertTrue(args.hasArgument("--disabled"));
        assertTrue(args.hasArgument("--force"));

        List<String> otherArgs = args.getOtherArguments();
        assertEquals(1, otherArgs.size());
        assertEquals("../../../../testsuite/smoke/target/deployments/test-deployment.sar", otherArgs.get(0));
    }

    protected ParsedArguments parse(String line) {
        DefaultParsedArguments args = new DefaultParsedArguments();
        try {
            args.parse(line);
        } catch (CommandFormatException e) {
            org.junit.Assert.fail(e.getLocalizedMessage());
        }
        return args;
    }
}
