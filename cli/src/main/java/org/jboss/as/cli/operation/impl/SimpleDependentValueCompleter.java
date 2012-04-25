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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.SimpleTabCompleter;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * This completer completes values of a parameter that depends on the value
 * of the other parameter whose value is the name of the resource property.
 * E.g. this completer can be used for parameter 'value' of the write-attribute
 * operation where parameter 'name' defines the value set for the parameter 'value'.
 *
 * @author Alexey Loubyansky
 */
public class SimpleDependentValueCompleter extends DefaultCompleter {

    private static final List<String> BOOLEAN = Arrays.asList(new String[]{"false", "true"});

    public SimpleDependentValueCompleter(final OperationRequestAddress address, final String idProperty) {
        super(new CandidatesProvider(){

            ModelNode allAttrs;

            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                final String propName = ctx.getParsedCommandLine().getPropertyValue(idProperty);
                if(propName == null) {
                    return Collections.emptyList();
                }

                if(allAttrs == null) {
                    final ModelNode req = new ModelNode();
                    final ModelNode addrNode = req.get(Util.ADDRESS);
                    for(OperationRequestAddress.Node node : address) {
                        addrNode.add(node.getType(), node.getName());
                    }
                    req.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
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
                    allAttrs = result.get(Util.ATTRIBUTES);
                }

                if(!allAttrs.isDefined()) {
                    return Collections.emptyList();
                }
                final ModelNode propDescr = allAttrs.get(propName);
                if(!propDescr.isDefined()) {
                    return Collections.emptyList();
                }

                final ModelNode typeNode = propDescr.get(Util.TYPE);
                if(typeNode.isDefined() && typeNode.asType().equals(ModelType.BOOLEAN)) {
                    return BOOLEAN;
                } else if(propDescr.has(Util.ALLOWED)) {
                    final ModelNode allowedNode = propDescr.get(Util.ALLOWED);
                    if(allowedNode.isDefined()) {
                        final List<ModelNode> nodeList = allowedNode.asList();
                        final List<String> values = new ArrayList<String>(nodeList.size());
                        for(ModelNode node : nodeList) {
                            values.add(node.asString());
                        }
                        return values;
                    }
                }
                return Collections.emptyList();
            }});
    }
}
