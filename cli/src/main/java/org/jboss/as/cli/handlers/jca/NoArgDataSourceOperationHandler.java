package org.jboss.as.cli.handlers.jca;

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BatchModeCommandHandler;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class NoArgDataSourceOperationHandler extends BatchModeCommandHandler {

    protected final String dsType;
    protected final String operationName;
    protected final ArgumentWithValue profile;
    protected final ArgumentWithValue name;

    public NoArgDataSourceOperationHandler(CommandContext ctx, String command, final String dsType, String operationName) {
        super(ctx, command, true);
        this.dsType = dsType;
        this.operationName = operationName;

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
                    profileArg = profile.getValue(ctx.getParsedCommandLine());
                    if (profileArg == null) {
                        return Collections.emptyList();
                        }
                    }

                return Util.getDatasources(ctx.getModelControllerClient(), profileArg, dsType);
                }
            }), 0, "--name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isValueComplete(ctx.getParsedCommandLine())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
        this.addRequiredPath("/subsystem=datasources");
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        if(ctx.isDomainMode()) {
            final String profile = this.profile.getValue(ctx.getParsedCommandLine());
            if(profile == null) {
                throw new OperationFormatException("Required argument --profile is missing.");
            }
            builder.addNode("profile", profile);
        }

        final String name = this.name.getValue(ctx.getParsedCommandLine(), true);

        builder.addNode("subsystem", "datasources");
        builder.addNode(dsType, name);
        builder.setOperationName(operationName);

        return builder.buildRequest();
    }
}
