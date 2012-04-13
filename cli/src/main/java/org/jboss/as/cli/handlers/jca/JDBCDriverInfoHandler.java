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
package org.jboss.as.cli.handlers.jca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BaseOperationCommand;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class JDBCDriverInfoHandler extends BaseOperationCommand {

    private final ArgumentWithValue name;

    public JDBCDriverInfoHandler(CommandContext ctx) {
        super(ctx, "jdbc-driver-info", true);
        addRequiredPath("/subsystem=datasources");

        name = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                try {
                    final ModelNode req = buildRequestWithoutHeaders(ctx);
                    final ModelNode response = ctx.getModelControllerClient().execute(req);
                    if(response.hasDefined(Util.RESULT)) {
                        final List<ModelNode> list = response.get(Util.RESULT).asList();
                        final List<String> names = new ArrayList<String>(list.size());
                        for(ModelNode node : list) {
                            if(node.hasDefined(Util.DRIVER_NAME)) {
                                names.add(node.get(Util.DRIVER_NAME).asString());
                            }
                        }
                        return names;
                    } else {
                        return Collections.emptyList();
                    }
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }}), 0, "--name");
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return !ctx.isDomainMode();
    }
    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        final ModelNode req = new ModelNode();
        final ModelNode address = req.get(Util.ADDRESS);
        address.add(Util.SUBSYSTEM, Util.DATASOURCES);
        req.get(Util.OPERATION).set(Util.INSTALLED_DRIVERS_LIST);
        return req;
    }

    @Override
    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) throws CommandLineException {
        final ModelNode result = response.get(Util.RESULT);
        if(!result.isDefined()) {
            throw new CommandLineException("The operation result is not defined: " + result);
        }
        final List<ModelNode> list = result.asList();
        if (!name.isPresent(ctx.getParsedCommandLine())) {
            final SimpleTable table = new SimpleTable(new String[] { "NAME", "SOURCE" });
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandLineException(Util.DRIVER_NAME + " is not available: " + node);
                }
                final String source;
                if (node.hasDefined(Util.DEPLOYMENT_NAME)) {
                    source = node.get(Util.DEPLOYMENT_NAME).asString();
                } else if (node.hasDefined(Util.DRIVER_MODULE_NAME)) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(node.get(Util.DRIVER_MODULE_NAME).asString());
                    if(node.hasDefined(Util.MODULE_SLOT)) {
                        buf.append('/').append(node.get(Util.MODULE_SLOT).asString());
                    }
                    source = buf.toString();
                } else {
                    source = "n/a";
                }
                table.addLine(new String[] { driverName.asString(), source });
            }
            ctx.printLine(table.toString(true));
        } else {
            final String name = this.name.getValue(ctx.getParsedCommandLine());
            final SimpleTable table = new SimpleTable(2);
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandLineException(Util.DRIVER_NAME + " is not available: " + node);
                }
                if(name.equals(driverName.asString())) {
                    for(String propName : node.keys()) {
                        final ModelNode value = node.get(propName);
                        table.addLine(new String[] { propName, value.isDefined() ? value.asString() : "n/a" });
                    }
                }
            }
            ctx.printLine(table.toString(false));
        }
    }
}
