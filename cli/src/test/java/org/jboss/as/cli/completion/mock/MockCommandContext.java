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
package org.jboss.as.cli.completion.mock;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.PrefixFormatter;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 *
 * @author Alexey Loubyansky
 */
public class MockCommandContext implements CommandContext {

    private ModelControllerClient mcc;
    private OperationRequestParser operationParser;
    private OperationRequestAddress prefix;
    private PrefixFormatter prefixFormatter;
    private OperationCandidatesProvider operationCandidatesProvider;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getCommandArguments()
     */
    @Override
    public String getCommandArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#printLine(java.lang.String)
     */
    @Override
    public void printLine(String message) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#printColumns(java.util.Collection<java.lang.String>)
     */
    @Override
    public void printColumns(Collection<String> col) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#terminateSession()
     */
    @Override
    public void terminateSession() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#set(java.lang.String, java.lang.Object)
     */
    @Override
    public void set(String key, Object value) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#get(java.lang.String)
     */
    @Override
    public Object get(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getModelControllerClient()
     */
    @Override
    public ModelControllerClient getModelControllerClient() {
        return mcc;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getOperationRequestParser()
     */
    @Override
    public OperationRequestParser getOperationRequestParser() {
        if(operationParser == null) {
            operationParser = new DefaultOperationRequestParser();
        }
        return operationParser;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getPrefix()
     */
    @Override
    public OperationRequestAddress getPrefix() {
        if(prefix == null) {
            prefix = new DefaultOperationRequestAddress();
        }
        return prefix;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getPrefixFormatter()
     */
    @Override
    public PrefixFormatter getPrefixFormatter() {
        if(prefixFormatter == null) {
            prefixFormatter = new DefaultPrefixFormatter();
        }
        return prefixFormatter;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandContext#getOperationCandidatesProvider()
     */
    @Override
    public OperationCandidatesProvider getOperationCandidatesProvider() {
        if(operationCandidatesProvider == null) {
            operationCandidatesProvider = new DefaultOperationCandidatesProvider(this);
        }
        return operationCandidatesProvider;
    }

    public void setOperationCandidatesProvider(OperationCandidatesProvider provider) {
        this.operationCandidatesProvider = provider;
    }

    @Override
    public void connectController(String host, int port) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnectController() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getControllerHost() {
        return null;
    }

    @Override
    public int getControllerPort() {
        return -1;
    }

    @Override
    public CommandHistory getHistory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultControllerHost() {
        return null;
    }

    @Override
    public int getDefaultControllerPort() {
        return -1;
    }

    @Override
    public boolean hasSwitch(String switchName) {
        return false;
    }

    @Override
    public String getNamedArgument(String argName) {
        return null;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public boolean hasArguments() {
        return false;
    }

    @Override
    public Set<String> getArgumentNames() {
        return null;
    }

    @Override
    public boolean isBatchMode() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCommand() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BatchManager getBatchManager() {
        // TODO Auto-generated method stub
        return null;
    }
}
