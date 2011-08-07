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
package org.jboss.as.cli.handlers.jms;

import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BatchModeCommandHandler;
import org.jboss.as.cli.handlers.SimpleTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class JmsQueueAddHandler extends BatchModeCommandHandler {

    private final ArgumentWithValue name;
    private final ArgumentWithValue entries;
    private final ArgumentWithValue selector;
    private final ArgumentWithValue durable;
    private final ArgumentWithValue profile;

    public JmsQueueAddHandler() {
        super("jms-queue-add", true);

        profile = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                return Util.getNodeNames(ctx.getModelControllerClient(), null, "profile");
            }}), "--profile") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(!ctx.isDomainMode()) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        name = new ArgumentWithValue(this, /*0,*/ "--name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        entries = new ArgumentWithValue(this, "--entries");
        entries.addRequiredPreceding(name);

        selector = new ArgumentWithValue(this, "--selector");
        selector.addRequiredPreceding(name);

        durable = new ArgumentWithValue(this, new SimpleTabCompleter(new String[]{"false", "true"}), "--durable");
        durable.addRequiredPreceding(name);

        this.addRequiredPath("/subsystem=messaging");
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        ParsedArguments args = ctx.getParsedArguments();

        if(ctx.isDomainMode()) {
            String profile = this.profile.getValue(args);
            if(profile == null) {
                throw new OperationFormatException("--profile argument value is missing.");
            }
            builder.addNode("profile",profile);
        }

        final String name = this.name.getValue(args, true);

        builder.addNode("subsystem", "messaging");
        builder.addNode("jms-queue", name);
        builder.setOperationName("add");

        ModelNode entriesNode = builder.getModelNode().get("entries");
        final String entriesStr = this.entries.getValue(args);
        if(entriesStr == null) {
            entriesNode.add(name);
        } else {
            String[] split = entriesStr.split(",");
            for(int i = 0; i < split.length; ++i) {
                String entry = split[i].trim();
                if(!entry.isEmpty()) {
                    entriesNode.add(entry);
                }
            }
        }

        final String selector = this.selector.getValue(args);
        if(selector != null) {
            builder.addProperty("selector", selector);
        }

        final String durable = this.durable.getValue(args);
        if(durable != null) {
            builder.addProperty("durable", durable);
        }

        return builder.buildRequest();
    }
}
