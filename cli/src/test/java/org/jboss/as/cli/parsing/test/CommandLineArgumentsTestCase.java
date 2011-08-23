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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandLineArgumentsTestCase {

    @Test
    public void testDefault() throws Exception {

        ParsedCommandLine args = parse("deploy ../../../../testsuite/smoke/target/deployments/test-deployment.sar --name=my.sar --disabled --runtime-name=myrt.sar --force");
        assertTrue(args.hasProperties());
        assertTrue(args.hasProperty("--name"));
        assertTrue(args.hasProperty("--runtime-name"));
        assertTrue(args.hasProperty("--disabled"));
        assertTrue(args.hasProperty("--force"));

        List<String> otherArgs = args.getOtherProperties();
        assertEquals(1, otherArgs.size());
        assertEquals("../../../../testsuite/smoke/target/deployments/test-deployment.sar", otherArgs.get(0));

        assertNull(args.getOutputTarget());
    }

    @Test
    public void testOutputTarget() throws Exception {

        ParsedCommandLine args = parse("cmd --name=value value1 --name1 > output.target");
        assertTrue(args.hasProperties());
        assertTrue(args.hasProperty("--name"));
        assertEquals("value", args.getPropertyValue("--name"));
        assertTrue(args.hasProperty("--name1"));
        assertNull(args.getPropertyValue("--name1"));

        List<String> otherArgs = args.getOtherProperties();
        assertEquals(1, otherArgs.size());
        assertEquals("value1", otherArgs.get(0));

        assertEquals("output.target", args.getOutputTarget());
    }

    protected ParsedCommandLine parse(String line) {
        DefaultCallbackHandler args = new DefaultCallbackHandler();
        try {
            args.parse(null, line);
        } catch (CommandFormatException e) {
            e.printStackTrace();
            org.junit.Assert.fail(e.getLocalizedMessage());
        }
        return args;
    }
}
