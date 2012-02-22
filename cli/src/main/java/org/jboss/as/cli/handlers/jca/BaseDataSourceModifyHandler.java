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

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BaseOperationCommand;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.impl.RequestParamArgWithValue;
import org.jboss.as.cli.impl.RequestParameterArgument;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class BaseDataSourceModifyHandler extends BaseOperationCommand {

    private final String dsType;
    private final ArgumentWithValue profile;
    private final ArgumentWithValue jndiName;

    public BaseDataSourceModifyHandler(CommandContext ctx, String commandName, final String dsType) {
        super(ctx, commandName, true);

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

        //jndiName =  new RequiredRequestParamArg("jndi-name", this, "--jndi-name") {
        jndiName =  new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                return Util.getDatasources(ctx.getModelControllerClient(), profile.getValue(ctx.getParsedCommandLine()), dsType);
            }}), "--jndi-name") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isValueComplete(ctx.getParsedCommandLine())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        initArguments();
        this.addRequiredPath("/subsystem=datasources");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {

        ModelNode composite = new ModelNode();
        composite.get("operation").set("composite");
        composite.get("address").setEmptyList();
        ModelNode steps = composite.get("steps");

        setParams(ctx, steps);

        return composite;
    }

    @Override
    protected void setParams(CommandContext ctx, ModelNode request) throws CommandFormatException {

        ParsedCommandLine args = ctx.getParsedCommandLine();

        final String profile;
        if(ctx.isDomainMode()) {
            profile = this.profile.getValue(args);
            if(profile == null) {
                throw new OperationFormatException("--profile argument value is missing.");
            }
        } else {
            profile = null;
        }

        for(RequestParameterArgument arg : params) {

            String value = arg.getValue(args);
            if (value != null) {
                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
                if (profile != null) {
                    builder.addNode("profile", profile);
                }

                builder.addNode("subsystem", "datasources");
                builder.addNode(dsType, jndiName.getValue(args, true));
                builder.setOperationName("write-attribute");
                builder.addProperty("name", arg.getPropertyName());
                builder.addProperty("value", value);
                request.add(builder.buildRequest());
            }
        }
    }

    private void initArguments() {
        initOptionalArguments(jndiName);
    }

    protected void initOptionalArguments(ArgumentWithoutValue lastRequired) {

        RequestParamArgWithValue maxPoolSize = new RequestParamArgWithValue("max-pool-size", this, new ValueCompleter("max-pool-size"));
        maxPoolSize.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue minPoolSize = new RequestParamArgWithValue("min-pool-size", this, new ValueCompleter("min-pool-size"));
        minPoolSize.addRequiredPreceding(lastRequired);

/*        RequestParamArgWithValue disabled = new RequestParamArgWithValue("enabled", this, "--disabled") {
            @Override
            public boolean isValueRequired() {
                return false;
            }
            @Override
            public void set(ParsedArguments args, ModelNode request) throws CommandFormatException {
                if(isPresent(args)) {
                    setValue(request, "enabled", "false");
                }
            }
        };
        disabled.addRequiredPreceding(lastRequired);
*/

        RequestParamArgWithValue poolPrefill =  new RequestParamArgWithValue("pool-prefill", this, new ValueCompleter("pool-prefill"));
        poolPrefill.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue poolUseStrictMin =  new RequestParamArgWithValue("pool-use-strict-min", this, new ValueCompleter("pool-use-strict-min"));
        poolUseStrictMin.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue blockingTimeoutWait =  new RequestParamArgWithValue("blocking-timeout-wait-millis", this, new ValueCompleter("blocking-timeout-wait-millis"));
        blockingTimeoutWait.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue idleTimeout =  new RequestParamArgWithValue("idle-timeout-minutes", this, new ValueCompleter("idle-timeout-minutes"));
        idleTimeout.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue backgroundValidation =  new RequestParamArgWithValue("background-validation", this, new ValueCompleter("background-validation"));
        backgroundValidation.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue backgroundValidationMins =  new RequestParamArgWithValue("background-validation-minutes", this, new ValueCompleter("background-validation-minutes"));
        backgroundValidationMins.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue useFastFail =  new RequestParamArgWithValue("use-fast-fail", this,  new ValueCompleter("use-fast-fail"));
        useFastFail.addRequiredPreceding(lastRequired);
    }

    public class ValueCompleter extends DefaultCompleter {

        public ValueCompleter(final String attrName) {
            super(new CandidatesProvider() {
                @Override
                public List<String> getAllCandidates(CommandContext ctx) {
                    ModelControllerClient client = ctx.getModelControllerClient();
                    if(client == null) {
                        return Collections.emptyList();
                    }

                    DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
                    if (ctx.isDomainMode()) {
                        final String profileName = profile.getValue(ctx.getParsedCommandLine());
                        if (profileName == null) {
                            return Collections.emptyList();
                        }
                        builder.addNode("profile", profileName);
                    }
                    builder.addNode("subsystem", "datasources");

                    final String dsName = jndiName.getValue(ctx.getParsedCommandLine());
                    if(dsName == null) {
                        return Collections.emptyList();
                    }
                    builder.addNode(dsType, dsName);

                    builder.setOperationName("read-attribute");
                    builder.addProperty("name", attrName);

                    try {
                        ModelNode result = client.execute(builder.buildRequest());
                        if(!result.hasDefined("result"))
                            return Collections.emptyList();
                        return Collections.singletonList(result.get("result").asString());
                    } catch (Exception e) {
                        return Collections.emptyList();
                    }
                }});
        }
    }
}
