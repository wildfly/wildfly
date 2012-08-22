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
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyNameCompleter extends DefaultCompleter {

    public PropertyNameCompleter(final OperationRequestAddress address, final boolean onlyWritable) {
        super(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
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
                final ModelNode attrs;
                if(result.getType().equals(ModelType.LIST)) {
                    ModelNode wildcardResult = null;
                    // wildcard address
                    for(ModelNode item : result.asList()) {
                        final ModelNode addr = item.get(Util.ADDRESS);
                        if(!addr.getType().equals(ModelType.LIST)) {
                            return Collections.emptyList();
                        }
                        for(ModelNode node : addr.asList()) {
                            if(!node.getType().equals(ModelType.PROPERTY)) {
                                throw new IllegalArgumentException(node.getType().toString());
                            }
                            if("*".equals(node.asProperty().getValue().asString())) {
                                wildcardResult = item;
                                break;
                            }
                        }
                        if(wildcardResult != null) {
                            break;
                        }
                    }
                    if(wildcardResult == null) {
                        throw new IllegalStateException("Failed to locate the wildcard result.");
                    }
                    wildcardResult = wildcardResult.get(Util.RESULT);
                    if(!wildcardResult.isDefined()) {
                        return Collections.emptyList();
                    }
                    attrs = wildcardResult.get(Util.ATTRIBUTES);
                } else {
                    attrs = result.get(Util.ATTRIBUTES);
                }
                if(!attrs.isDefined()) {
                    return Collections.emptyList();
                }
                final List<String> names = new ArrayList<String>();
                for(Property attr : attrs.asPropertyList()) {
                    if(onlyWritable) {
                        final ModelNode descr = attr.getValue();
                        if(!descr.has(Util.ACCESS_TYPE) || !Util.READ_ONLY.equals(descr.get(Util.ACCESS_TYPE).asString())) {
                            names.add(attr.getName());
                        }
                    } else {
                        names.add(attr.getName());
                    }
                }
                return names;
            }});
    }
}
