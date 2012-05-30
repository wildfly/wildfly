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

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.dmr.ModelNode;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class LinebreaksTestCase {

    private static final String LN = System.getProperty("line.separator");
    private static CommandContext ctx;

    private final String[] headers = new String[]{"{","rollout"," main-server-group", "(",
            "max-failed-servers","=","2",",",
            "rolling-to-servers","=","true",")",",",
            "other-server-group",
            " rollback-across-groups",";",
            "rollback-on-runtime-failure","=","true",
            "}"};

    private final String[] op = new String[]{
            ":",
            "read-resource","(",
            "include-defaults","=","true",",",
            "recursive","=","false",")"
    };

    private final String[] cmd = new String[]{
            "read-attribute"," product-name",
            " --include-defaults","=","true",
            " --verbose",
            " --headers","="
    };

    @BeforeClass
    public static void setup() throws Exception {
        ctx = CommandContextFactory.getInstance().newCommandContext();
    }

    @Test
    public void testOp() throws Exception {
        testPieces(op);
    }

    @Test
    public void testCmd() throws Exception {
        testPieces(cmd);
    }

    protected void testPieces(String[] arr) throws Exception {
        final StringBuilder buf = new StringBuilder();
        for(String line : arr) {
            buf.append(line);
        }
        for(String line : headers) {
            buf.append(line);
        }
        final String noLN = buf.toString();
        buf.setLength(0);
        buf.append(arr[0]);
        for(int i = 1; i < arr.length; ++i) {
            buf.append('\\').append(LN).append(arr[i]);
        }
        for(int i = 0; i < headers.length; ++i) {
            buf.append('\\').append(LN).append(headers[i]);
        }
        assertEquivalent(noLN, buf.toString());
    }

    protected void assertEquivalent(String line1, String line2) throws Exception {
        final ModelNode op1 = ctx.buildRequest(line1);
//        System.out.println(line2);
        final ModelNode op2 = ctx.buildRequest(line2);
        assertEquals(op1, op2);
//        System.out.println(op1);
//        System.out.println(op2);
    }
}
