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
package org.jboss.as.cli.operation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ChildTypeCompleter extends DefaultCompleter {

    public ChildTypeCompleter(final OperationRequestAddress address) {
        super(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final ModelNode req = new ModelNode();
                final ModelNode addrNode = req.get(Util.ADDRESS);
                for(OperationRequestAddress.Node node : address) {
                    addrNode.add(node.getType(), node.getName());
                }
                req.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
                final ModelNode response;
                try {
                    response = ctx.getModelControllerClient().execute(req);
                } catch (Exception e) {
                    return Collections.emptyList();
                }
                final ModelNode result = response.get(Util.RESULT);
                if(!result.isDefined()) {
                    return Collections.emptyList();
                }
                final List<ModelNode> list = result.asList();
                final List<String> names = new ArrayList<String>(list.size());
                for(ModelNode node : list) {
                    names.add(node.asString());
                }
                return names;
            }});
    }
}
