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

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class LinebreaksTestCase {

    private static final String LN = Util.LINE_SEPARATOR;
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
    @Ignore
    public void testOp() throws Exception {
        testPieces(op);
    }

    @Test
    @Ignore
    public void testCmd() throws Exception {
        testPieces(cmd);
    }

    @Test
    public void testLineBreaksAndTabs() throws Exception {
        final StringBuilder buf = new StringBuilder();
        buf.append("/subsystem=security/security-domain=DemoAuthRealm/authentication=classic:add( \\").append(Util.LINE_SEPARATOR);
        buf.append('\t').append("login-modules=[ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t").append("{ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("\"code\" => \"Database\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("flag=>required, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("test1 = > required, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("test2 =\"> required\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("\"module-options\" = [ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("\"unauthenticatedIdentity\"=\"guest\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("\"dsJndiName\"=\"java:jboss/jdbc/ApplicationDS\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("\"principalsQuery\"=\"select password from users where username=?\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("\"rolesQuery\"=\"select name, 'Roles' FROM user_roless ur, roles r, user u WHERE u.username=? and u.id = ur.user_id and ur.role_id = r.id\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("\"hashAlgorithm\" = \"MD5\", \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("hashEncoding = hex \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("] \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t").append("} \\").append(Util.LINE_SEPARATOR);
        buf.append('\t').append(']').append(Util.LINE_SEPARATOR);
        buf.append(')');

        final ModelNode node = ctx.buildRequest(buf.toString());
        ModelNode addr = new ModelNode();
        addr.add("subsystem", "security");
        addr.add("security-domain", "DemoAuthRealm");
        addr.add("authentication", "classic");
        assertEquals(addr, node.get(Util.ADDRESS));

        assertEquals("add", node.get(Util.OPERATION).asString());

        final ModelNode loginModules = node.get("login-modules");
        assertTrue(loginModules.isDefined());
        assertEquals(ModelType.LIST, loginModules.getType());
        List<ModelNode> list = loginModules.asList();
        assertEquals(1, list.size());
        final ModelNode module = list.get(0);
        assertTrue(module.isDefined());
        assertEquals("Database", module.get("code").asString());
        assertEquals("required", module.get("flag").asString());
        assertEquals("> required", module.get("test1").asString());
        assertEquals("> required", module.get("test2").asString());

        final ModelNode options = module.get("module-options");
        assertTrue(options.isDefined());
        assertEquals(ModelType.LIST, options.getType());
        list = options.asList();
        assertEquals(6, list.size());

        assertEquals("guest", list.get(0).get("unauthenticatedIdentity").asString());
        assertEquals("java:jboss/jdbc/ApplicationDS", list.get(1).get("dsJndiName").asString());
        assertEquals("select password from users where username=?", list.get(2).get("principalsQuery").asString());
        assertEquals("select name, 'Roles' FROM user_roless ur, roles r, user u WHERE u.username=? and u.id = ur.user_id and ur.role_id = r.id", list.get(3).get("rolesQuery").asString());
        assertEquals("MD5", list.get(4).get("hashAlgorithm").asString());
        assertEquals("hex", list.get(5).get("hashEncoding").asString());
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
