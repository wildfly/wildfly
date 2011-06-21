package org.jboss.as.cli.handlers.jca;

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BatchModeCommandHandler;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class SimpleDataSourceOperationHandler extends BatchModeCommandHandler {

    protected final String dsType;
    protected final ArgumentWithValue profile;
    protected final ArgumentWithValue name;
    protected final ArgumentWithValue operation;

    public SimpleDataSourceOperationHandler(final String dsType) {
        super("data-source", true);
        this.dsType = dsType;

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

        name = new ArgumentWithValue(this, new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                ModelControllerClient client = ctx.getModelControllerClient();
                if (client == null) {
                    return Collections.emptyList();
                    }

                final String profileArg;
                if (!ctx.isDomainMode()) {
                    profileArg = null;
                } else {
                    profileArg = profile.getValue(ctx.getParsedArguments());
                    if (profileArg == null) {
                        return Collections.emptyList();
                        }
                    }

                return Util.getDatasources(ctx.getModelControllerClient(), profileArg, dsType);
                }
            }), 0, "--name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        operation = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final StringBuilder buf = new StringBuilder();
                if(ctx.isDomainMode()) {
                    final String profileName = profile.getValue(ctx.getParsedArguments());
                    if(profile == null) {
                        return -1;
                    }
                    buf.append("profile=").append(profileName);
                }
                buf.append("subsystem=datasources/").append(dsType).append('=');
                final String dsName = name.getValue(ctx.getParsedArguments());
                if(dsName == null) {
                    return -1;
                }
                buf.append(Util.escapeString(dsName, OperationRequestCompleter.ESCAPE_SELECTOR)).append(':');
                final int addressLength = buf.length();
                cursor += addressLength;
                buf.append(buffer);
                int result = OperationRequestCompleter.INSTANCE.complete(ctx, buf.toString(), cursor, candidates) - addressLength;
                return result;
            }}, 1, "--operation") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        operation.addRequiredPreceding(name);
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        if(ctx.isDomainMode()) {
            final String profile = this.profile.getValue(ctx.getParsedArguments());
            if(profile == null) {
                throw new OperationFormatException("Required argument --profile is missing.");
            }
            builder.addNode("profile", profile);
        }

        final String name = this.name.getValue(ctx.getParsedArguments(), true);

        final String operation = this.operation.getValue(ctx.getParsedArguments(), true);

        builder.addNode("subsystem", "datasources");
        builder.addNode(dsType, name);
        builder.setOperationName(operation);

        return builder.buildRequest();
    }
}
