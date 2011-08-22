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
package org.jboss.as.cli.completion.operation.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.completion.mock.MockNode;
import org.jboss.as.cli.completion.mock.MockOperation;
import org.jboss.as.cli.completion.mock.MockOperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationNameCompletionTestCase {

    private MockCommandContext ctx;
    private OperationRequestCompleter completer;

    public OperationNameCompletionTestCase() {

        MockNode root = new MockNode("root");

        MockOperation op = new MockOperation("operation-no-properties");
        root.addOperation(op);

        op = new MockOperation("operation-property-a");
        root.addOperation(op);

        op = new MockOperation("operation-properties-a-b");
        root.addOperation(op);

        ctx = new MockCommandContext();
        ctx.setOperationCandidatesProvider(new MockOperationCandidatesProvider(root));
        completer = new OperationRequestCompleter();
    }

    @Test
    public void testAllCandidates() {

        List<String> candidates = fetchCandidates(":");
        assertNotNull(candidates);
        assertEquals(Arrays.asList("operation-no-properties", "operation-properties-a-b", "operation-property-a"), candidates);
    }

    @Test
    public void testSelectedCandidates() {

        List<String> candidates = fetchCandidates(":operation-p");
        assertNotNull(candidates);
        assertEquals(Arrays.asList("operation-properties-a-b", "operation-property-a"), candidates);
    }

    @Test
    public void testNoMatch() {

        List<String> candidates = fetchCandidates(":no-match");
        assertNotNull(candidates);
        assertEquals(Arrays.asList(), candidates);
    }

    protected List<String> fetchCandidates(String buffer) {
        ArrayList<String> candidates = new ArrayList<String>();
        try {
            ctx.parseCommandLine(buffer);
        } catch (CommandFormatException e) {
            return Collections.emptyList();
        }
        completer.complete(ctx, buffer, 0, candidates);
        return candidates;
    }
}
