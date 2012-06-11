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

import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.dmr.ModelNode;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class SimplifiedOperationParameterFormatTestCase {

    private final MockCommandContext ctx = new MockCommandContext();
    private final OperationRequestAddress rootAddr = new DefaultOperationRequestAddress();
    private final DefaultCallbackHandler handler = new DefaultCallbackHandler(false);

    @Test
    public void testSimpleList() throws Exception {
        assertEquivalent("[\"a\",\"b\"]", "[a,b]");
    }

    @Test
    public void testSimpleObject() throws Exception {
        assertEquivalent("{\"a\"=>\"b\",\"c\"=>\"d\"}", "{a=b,c=d}");
    }

    @Test
    public void testVeryVerySimpleObject() throws Exception {
        assertEquivalent("{\"a\"=>\"b\"}", "{a=b}");
    }

    @Test
    public void testPropertyList() throws Exception {
        assertEquivalent("[(\"a\"=>\"b\"),(\"c\"=>\"d\")]", "[a=b,c=d]");
    }

    @Test
    public void testMix() throws Exception {
        assertEquivalent("{\"a\"=>\"b\",\"c\"=>[\"d\",\"e\"],\"f\"=>[(\"g\"=>\"h\"),(\"i\"=>\"j\")]}", "{a=b,c=[d,e],f=[g=h,i=j]}");
    }

    protected void assertEquivalent(String dmrParams, String simplifiedParams) throws Exception {
        handler.parseOperation(rootAddr, ":test(test=" + dmrParams + ")");
        final ModelNode dmrReq = handler.toOperationRequest(ctx);
        handler.parseOperation(rootAddr, ":test(test=" + simplifiedParams + ")");
        final ModelNode simplifiedReq = handler.toOperationRequest(ctx);
        assertEquals(dmrReq, simplifiedReq);
    }
}
