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
package org.jboss.as.cli.completion.address.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.completion.mock.MockNode;
import org.jboss.as.cli.completion.mock.MockOperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestCompleter;


/**
*
* @author Alexey Loubyansky
*/
public class AbstractAddressCompleterTest {

    protected MockCommandContext ctx;
    protected OperationRequestCompleter completer;
    protected MockNode root = new MockNode("root");

    public AbstractAddressCompleterTest() {
        super();
        init();
    }

    protected void init() {
        ctx = new MockCommandContext();
        ctx.setOperationCandidatesProvider(new MockOperationCandidatesProvider(root));
        completer = new OperationRequestCompleter();
    }

    protected List<String> fetchCandidates(String buffer) {
        List<String> candidates = new ArrayList<String>();
        try {
            ctx.parseCommandLine(buffer);
        } catch (CommandFormatException e) {
//            System.out.println(ctx.getPrefixFormatter().format(ctx.getPrefix()) + ", '" + buffer + "'");
//            e.printStackTrace();
            return Collections.emptyList();
        }
        completer.complete(ctx, buffer, 0, candidates);
        return candidates;
    }

    protected MockNode addRoot(String name) {
        return root.addChild(name);
    }

    protected MockNode removeRoot(String name) {
        return root.remove(name);
    }
}