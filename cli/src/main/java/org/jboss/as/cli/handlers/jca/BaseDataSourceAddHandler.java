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
import org.jboss.as.cli.handlers.SimpleTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.impl.RequestParamArgWithValue;
import org.jboss.as.cli.impl.RequestParamArgWithoutValue;
import org.jboss.as.cli.impl.RequestParamPropertiesArg;
import org.jboss.as.cli.impl.RequiredRequestParamArg;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class BaseDataSourceAddHandler extends BaseOperationCommand {

    private final String dsType;
    private final ArgumentWithValue profile;
    private final ArgumentWithValue jndiName;

    public BaseDataSourceAddHandler(CommandContext ctx, String commandName, String dsType) {
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

        jndiName =  new RequiredRequestParamArg("jndi-name", this, "--jndi-name") {
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

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        ParsedCommandLine args = ctx.getParsedCommandLine();

        if(ctx.isDomainMode()) {
            String profile = this.profile.getValue(args);
            if(profile == null) {
                throw new OperationFormatException("--profile argument value is missing.");
            }
            builder.addNode("profile",profile);
        }

        builder.addNode("subsystem", "datasources");
        builder.addNode(dsType, jndiName.getValue(args, true));
        builder.setOperationName("add");

        setParams(ctx, builder.getModelNode());

        return builder.buildRequest();
    }

    private void initArguments() {
        ArgumentWithoutValue lastRequired = initRequiredArguments();
        initOptionalArguments(lastRequired);
    }

    protected ArgumentWithoutValue initRequiredArguments() {
        /*
        driverClass = new ArgumentWithValue(this, "--driver-class") {
        @Override
        public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
            if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                return false;
            }
            return super.canAppearNext(ctx);
        }
        };
        */

        RequestParamArgWithValue driverName = new RequiredRequestParamArg("driver-name", this,
                new DefaultCompleter(new CandidatesProvider() {
                    @Override
                    public List<String> getAllCandidates(CommandContext ctx) {
                        final String profileName;
                        if (ctx.isDomainMode()) {
                            profileName = profile.getValue(ctx.getParsedCommandLine());
                            if (profileName == null) {
                                return Collections.emptyList();
                            }
                        } else {
                            profileName = null;
                        }

                        OperationRequestAddress datasources = new DefaultOperationRequestAddress();
                        if (profileName != null) {
                            datasources.toNode("profile", profileName);
                        }
                        datasources.toNode("subsystem", "datasources");
                        return Util.getNodeNames(
                                ctx.getModelControllerClient(), datasources,
                                "jdbc-driver");
                    }
                }));
        driverName.addRequiredPreceding(jndiName);

        RequestParamArgWithValue poolName = new RequiredRequestParamArg("pool-name", this, "--pool-name");
        poolName.addRequiredPreceding(driverName);

        return poolName;
    }

    protected void initOptionalArguments(ArgumentWithoutValue lastRequired) {
        RequestParamArgWithValue username = new RequestParamArgWithValue("user-name", this, "--username");
        username.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue password =  new RequestParamArgWithValue("password", this, "--password");
        password.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue useJavaContext =  new RequestParamArgWithoutValue("use-java-context", this);
        useJavaContext.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue maxPoolSize = new RequestParamArgWithValue("max-pool-size", this);
        maxPoolSize.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue minPoolSize = new RequestParamArgWithValue("min-pool-size", this);
        minPoolSize.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue disabled = new RequestParamArgWithValue("enabled", this, "--disabled") {
            @Override
            public boolean isValueRequired() {
                return false;
            }
            @Override
            public void set(ParsedCommandLine args, ModelNode request) throws CommandFormatException {
                if(isPresent(args)) {
                    setValue(request, "enabled", "false");
                }
            }
        };
        disabled.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue newConnectionSql = new RequestParamArgWithValue("new-connection-sql", this);
        newConnectionSql.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue urlDelimiter = new RequestParamArgWithValue("url-delimiter", this);
        urlDelimiter.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue urlSelectorStrategyClass = new RequestParamArgWithValue("url-selector-strategy-class-name", this, "--url-selector-strategy-class");
        urlSelectorStrategyClass.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue poolPrefill =  new RequestParamArgWithoutValue("pool-prefill", this);
        poolPrefill.addRequiredPreceding(lastRequired);
        RequestParamArgWithoutValue poolUseStrictMin =  new RequestParamArgWithoutValue("pool-use-strict-min", this);
        poolUseStrictMin.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue flushStrategy =  new RequestParamArgWithValue("flush-strategy", this, new SimpleTabCompleter(new String[]{"FAILING_CONNECTION_ONLY", "IDLE_CONNECTIONS", "ENTIRE_POOL"}));
        flushStrategy.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue securityDomain = new RequestParamArgWithValue("security-domain", this);
        securityDomain.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue reauthPluginClass = new RequestParamArgWithValue("reauth-plugin-class-name", this);
        reauthPluginClass.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue reauthPluginProps = new RequestParamPropertiesArg("reauth-plugin-properties", this);
        reauthPluginProps.addRequiredPreceding(reauthPluginClass);

        RequestParamArgWithoutValue sharePreparedStatements =  new RequestParamArgWithoutValue("share-prepared-statements", this);
        sharePreparedStatements.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue psCacheSize = new RequestParamArgWithValue("prepared-statements-cacheSize", this, "--prepared-statements-cache-size");
        psCacheSize.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue trackStatements =  new RequestParamArgWithValue("track-statements", this, new SimpleTabCompleter(new String[]{"FALSE", "NOWARN", "TRUE"}));
        trackStatements.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue allocationRetry =  new RequestParamArgWithValue("allocation-retry", this);
        allocationRetry.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue allocationRetryWait =  new RequestParamArgWithValue("allocation-retry-wait-millis", this);
        allocationRetryWait.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue blockingTimeoutWait =  new RequestParamArgWithValue("blocking-timeout-wait-millis", this);
        blockingTimeoutWait.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue idleTimeout =  new RequestParamArgWithValue("idle-timeout-minutes", this);
        idleTimeout.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue queryTimeout =  new RequestParamArgWithValue("query-timeout", this);
        queryTimeout.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue useTryLock =  new RequestParamArgWithValue("use-try-lock", this);
        useTryLock.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue setTxQueryTimeout =  new RequestParamArgWithValue("set-tx-query-timeout", this);
        setTxQueryTimeout.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue txIsolation =  new RequestParamArgWithValue("transaction-isolation", this, new SimpleTabCompleter(new String[]{"TRANSACTION_READ_UNCOMMITTED", "TRANSACTION_READ_COMMITTED", "TRANSACTION_REPEATABLE_READ", "TRANSACTION_SERIALIZABLE", "TRANSACTION_NONE"}));
        txIsolation.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue checkValidConnectionSql =  new RequestParamArgWithValue("check-valid-connection-sql", this);
        checkValidConnectionSql.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue exceptionSorterClass =  new RequestParamArgWithValue("exception-sorter-class-name", this, "--exception-sorter-class");
        exceptionSorterClass.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue exceptionSorterProps =  new RequestParamPropertiesArg("exception-sorter-properties", this);
        exceptionSorterProps.addRequiredPreceding(exceptionSorterClass);

        RequestParamArgWithValue staleConnectionCheckerClass =  new RequestParamArgWithValue("stale-connection-checker-class-name", this, "--stale-connection-checker-class");
        staleConnectionCheckerClass.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue staleConnectionCheckerProps =  new RequestParamPropertiesArg("stale-connection-checker-properties", this);
        staleConnectionCheckerProps.addRequiredPreceding(staleConnectionCheckerClass);

        RequestParamArgWithValue validConnectionCheckerClass =  new RequestParamArgWithValue("valid-connection-checker-class-name", this, "--valid-connection-checker-class");
        validConnectionCheckerClass.addRequiredPreceding(lastRequired);
        RequestParamArgWithValue validConnectionCheckerProps =  new RequestParamPropertiesArg("valid-connection-checker-properties", this);
        validConnectionCheckerProps.addRequiredPreceding(validConnectionCheckerClass);

        RequestParamArgWithoutValue backgroundValidation =  new RequestParamArgWithoutValue("background-validation", this);
        backgroundValidation.addRequiredPreceding(lastRequired);

        RequestParamArgWithValue backgroundValidationMins =  new RequestParamArgWithValue("background-validation-minutes", this);
        backgroundValidationMins.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue useFastFail =  new RequestParamArgWithoutValue("use-fast-fail", this);
        useFastFail.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue validateOnMatch =  new RequestParamArgWithoutValue("validate-on-match", this);
        validateOnMatch.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue spy =  new RequestParamArgWithoutValue("spy", this);
        spy.addRequiredPreceding(lastRequired);

        RequestParamArgWithoutValue useCCM =  new RequestParamArgWithoutValue("use-ccm", this);
        useCCM.addRequiredPreceding(lastRequired);
    }
}
