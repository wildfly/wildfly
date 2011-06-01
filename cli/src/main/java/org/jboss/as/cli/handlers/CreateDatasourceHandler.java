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
package org.jboss.as.cli.handlers;

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.impl.RequestParamArg;
import org.jboss.as.cli.impl.RequiredRequestParamArg;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class CreateDatasourceHandler extends BaseOperationCommand {

    private final ArgumentWithValue profile;
    private final ArgumentWithValue jndiName;

//TODO    private final ArgumentWithValue connectionProperties;
//TODO    private final ArgumentWithValue driverClass;

    //TODO    private final ArgumentWithValue reauthPluginProps;
    // TODO private final ArgumentWithValue exceptionSorterProps;
    // TODO private final ArgumentWithValue staleConnectionCheckerProps;
    // TODO private final ArgumentWithValue validConnectionCheckerProps;

    public CreateDatasourceHandler() {
        super("create-datasource", true);

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

/*        driverClass = new ArgumentWithValue(this, "--driver-class") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };
*/
        RequestParamArg driverName = new RequiredRequestParamArg("driver-name", this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public List<String> getAllCandidates(CommandContext ctx) {
                final String profileName;
                if(ctx.isDomainMode()) {
                    profileName = profile.getValue(ctx.getParsedArguments());
                    if(profileName == null) {
                        return Collections.emptyList();
                    }
                } else {
                    profileName = null;
                }

                OperationRequestAddress datasources = new DefaultOperationRequestAddress();
                if(profileName != null) {
                    datasources.toNode("profile", profileName);
                }
                datasources.toNode("subsystem", "datasources");
                return Util.getNodeNames(ctx.getModelControllerClient(), datasources, "jdbc-driver");
            }})) {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if(ctx.isDomainMode() && !profile.isPresent(ctx.getParsedArguments())) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        RequestParamArg connectionUrl = new RequiredRequestParamArg("connection-url", this, "--connection-url");
        connectionUrl.addRequiredPreceding(driverName);

        RequestParamArg username = new RequiredRequestParamArg("user-name", this, "--username");
        username.addRequiredPreceding(connectionUrl);

        RequestParamArg password =  new RequiredRequestParamArg("password", this, "--password");
        password.addRequiredPreceding(username);

        RequestParamArg poolName =  new RequiredRequestParamArg("pool-name", this, "--pool-name");
        poolName.addRequiredPreceding(password);

        jndiName =  new RequiredRequestParamArg("jndi-name", this, "--jndi-name");
        jndiName.addRequiredPreceding(poolName);

        CommandArgument lastRequired = jndiName;

        RequestParamArg useJavaContext =  new RequestParamArg("use-java-context", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        useJavaContext.addRequiredPreceding(lastRequired);

        RequestParamArg maxPoolSize = new RequestParamArg("max-pool-size", this);
        maxPoolSize.addRequiredPreceding(lastRequired);

        RequestParamArg minPoolSize = new RequestParamArg("min-pool-size", this);
        minPoolSize.addRequiredPreceding(lastRequired);

        RequestParamArg disabled = new RequestParamArg("enabled", this, "--disabled") {
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

        RequestParamArg newConnectionSql = new RequestParamArg("new-connection-sql", this);
        newConnectionSql.addRequiredPreceding(lastRequired);

        RequestParamArg urlDelimiter = new RequestParamArg("url-delimiter", this);
        urlDelimiter.addRequiredPreceding(lastRequired);
        RequestParamArg urlSelectorStrategyClass = new RequestParamArg("url-selector-strategy-class", this, "--url-selector-strategy-class-name");
        urlSelectorStrategyClass.addRequiredPreceding(lastRequired);

        RequestParamArg poolPrefill =  new RequestParamArg("pool-prefill", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        poolPrefill.addRequiredPreceding(lastRequired);
        RequestParamArg poolUseStrictMin =  new RequestParamArg("pool-use-strict-min", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        poolUseStrictMin.addRequiredPreceding(lastRequired);

        RequestParamArg flushStrategy =  new RequestParamArg("flush-strategy", this, new SimpleTabCompleter(new String[]{"FailingConnectionOnly", "IdleConnections", "EntirePool"}));
        flushStrategy.addRequiredPreceding(lastRequired);

        RequestParamArg securityDomain = new RequestParamArg("security-domain", this);
        securityDomain.addRequiredPreceding(lastRequired);

        RequestParamArg reauthPluginClass = new RequestParamArg("reauth-plugin-class-name", this);
        reauthPluginClass.addRequiredPreceding(lastRequired);

        RequestParamArg psCacheSize = new RequestParamArg("prepared-statements-cacheSize", this, "--prepared-statements-cache-size");
        psCacheSize.addRequiredPreceding(lastRequired);

        RequestParamArg trackStatements =  new RequestParamArg("track-statements", this, new SimpleTabCompleter(new String[]{"FALSE", "NOWARN", "TRUE"}));
        trackStatements.addRequiredPreceding(lastRequired);

        RequestParamArg allocationRetry =  new RequestParamArg("allocation-retry", this);
        allocationRetry.addRequiredPreceding(lastRequired);

        RequestParamArg allocationRetryWait =  new RequestParamArg("allocation-retry-wait-millis", this);
        allocationRetryWait.addRequiredPreceding(lastRequired);

        RequestParamArg blockingTimeoutWait =  new RequestParamArg("blocking-timeout-wait-millis", this);
        blockingTimeoutWait.addRequiredPreceding(lastRequired);

        RequestParamArg idleTimeout =  new RequestParamArg("idle-timeout-minutes", this);
        idleTimeout.addRequiredPreceding(lastRequired);

        RequestParamArg queryTimeout =  new RequestParamArg("query-timeout", this);
        queryTimeout.addRequiredPreceding(lastRequired);

        RequestParamArg xaResourceTimeout =  new RequestParamArg("xa-resource-timeout", this);
        xaResourceTimeout.addRequiredPreceding(lastRequired);

        RequestParamArg useTryLock =  new RequestParamArg("use-try-lock", this);
        useTryLock.addRequiredPreceding(lastRequired);

        RequestParamArg setTxQueryTimeout =  new RequestParamArg("set-tx-query-timeout", this);
        setTxQueryTimeout.addRequiredPreceding(lastRequired);

        RequestParamArg txIsolation =  new RequestParamArg("transaction-isolation", this, new SimpleTabCompleter(new String[]{"TRANSACTION_READ_UNCOMMITTED", "TRANSACTION_READ_COMMITTED", "TRANSACTION_REPEATABLE_READ", "TRANSACTION_SERIALIZABLE", "TRANSACTION_NONE"}));
        txIsolation.addRequiredPreceding(lastRequired);

        RequestParamArg checkValidConnectionSql =  new RequestParamArg("check-valid-connection-sql", this);
        checkValidConnectionSql.addRequiredPreceding(lastRequired);

        RequestParamArg exceptionSorterClass =  new RequestParamArg("exception-sorter-class-name", this);
        exceptionSorterClass.addRequiredPreceding(lastRequired);

        RequestParamArg staleConnectionCheckerClass =  new RequestParamArg("stale-connection-checker-class-name", this);
        staleConnectionCheckerClass.addRequiredPreceding(lastRequired);

        RequestParamArg validConnectionCheckerClass =  new RequestParamArg("valid-connection-checker-class-name", this);
        validConnectionCheckerClass.addRequiredPreceding(lastRequired);

        RequestParamArg backgroundValidation =  new RequestParamArg("background-validation", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        backgroundValidation.addRequiredPreceding(lastRequired);

        RequestParamArg backgroundValidationMins =  new RequestParamArg("background-validation-minutes", this);
        backgroundValidationMins.addRequiredPreceding(lastRequired);

        RequestParamArg useFastFail =  new RequestParamArg("use-fast-fail", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        useFastFail.addRequiredPreceding(lastRequired);

        RequestParamArg validateOnMatch =  new RequestParamArg("validate-on-match", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        validateOnMatch.addRequiredPreceding(lastRequired);

        RequestParamArg spy =  new RequestParamArg("spy", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        spy.addRequiredPreceding(lastRequired);

        RequestParamArg useCCM =  new RequestParamArg("use-ccm", this, SimpleTabCompleter.BOOLEAN_COMPLETER);
        useCCM.addRequiredPreceding(lastRequired);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
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

        builder.addNode("subsystem", "datasources");
        builder.addNode("data-source", jndiName.getValue(args, true));
        builder.setOperationName("add");

        setParams(args, builder.getModelNode());

        return builder.buildRequest();
    }
}
