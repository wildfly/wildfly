/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.controller.audit;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;

/**
 * Audit logger wrapper
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 * @author Kabir Khan
 */
public class ManagedAuditLoggerImpl implements ManagedAuditLogger, ManagedAuditLogger.AuditLogHandlerUpdater {
    /** Maximum number of consecutive logging failures before we stop logging */
    private static final short MAX_FAILURE_COUNT = 10;

    private final List<ManagedAuditLoggerImpl> childImpls;

    /** If we are the core audit logger, list the children */
    private final ManagedAuditLogConfiguration config;

    /** Guarded by config's auditLock - updates to the handlers */
    private HandlerUpdateTask handlerUpdateTask;

    /** Guarded by config's auditLock - the messages logged while in the QUEUEING state */
    private final List<AuditLogItem> queuedItems = new ArrayList<AuditLogItem>();

    /** Guarded by config's auditLock - the number of failures writing to the log */
    private short failureCount;

    public ManagedAuditLoggerImpl(String asVersion, boolean server) {
        config = new CoreAuditLogConfiguration(asVersion, server);
        childImpls = new ArrayList<ManagedAuditLoggerImpl>();
    }

    private ManagedAuditLoggerImpl(ManagedAuditLoggerImpl src, boolean manualCommit) {
        assert src.config instanceof CoreAuditLogConfiguration : "Not an instance of CoreAuditLogConfiguration";
        config = new NewAuditLogConfiguration((CoreAuditLogConfiguration)src.config, manualCommit);
        childImpls = null;
    }

    @Override
    public void log(boolean readOnly, ResultAction resultAction, String userId, String domainUUID, AccessMechanism accessMechanism,
            InetAddress remoteAddress, Resource resultantModel, List<ModelNode> operations) {
        config.lock();
        try {
            if (config.isBooting() && !isLogBoot()) {
                return;
            }
            if (readOnly && !isLogReadOnly()) {
                return;
            }
            storeLogItem(
                    AuditLogItem.createModelControllerItem(config.getAsVersion(), readOnly, config.isBooting(), resultAction, userId, domainUUID,
                            accessMechanism, remoteAddress, resultantModel, operations));
        } catch (Exception e) {
            handleLoggingException(e);
        } finally {
            applyHandlerUpdates();
            config.unlock();
        }

    }

    @Override
    public void logJmxMethodAccess(boolean readOnly, String userId, String domainUUID, AccessMechanism accessMechanism,
            InetAddress remoteAddress, String methodName, String[] methodSignature, Object[] methodParams, Throwable error) {
        config.lock();
        try {
            if (config.isBooting() && !isLogBoot()) {
                return;
            }
            if (readOnly && !isLogReadOnly()) {
                return;
            }
            storeLogItem(
                    AuditLogItem.createMethodAccessItem(config.getAsVersion(), readOnly, config.isBooting(), userId, domainUUID, accessMechanism,
                            remoteAddress, methodName, methodSignature, methodParams, error));
        } catch (Exception e) {
            handleLoggingException(e);
        } finally {
            applyHandlerUpdates();
            config.unlock();
        }
    }

    public ManagedAuditLoggerImpl createNewConfiguration(boolean manualCommit) {
        if (childImpls == null) {
            throw ControllerMessages.MESSAGES.canOnlyCreateChildAuditLoggerForMainAuditLogger();
        }
        ManagedAuditLoggerImpl child = new ManagedAuditLoggerImpl(this, manualCommit);
        childImpls.add(child);
        return child;
    }

    public boolean isLogReadOnly() {
        config.lock();
        try {
            return config.isLogReadOnly();
        } finally {
            config.unlock();
        }
    }

    public void setLogReadOnly(boolean logReadOnly) {
        config.lock();
        try {
            config.setLogReadOnly(logReadOnly);
        } finally {
            config.unlock();
        }
    }

    public boolean isLogBoot() {
        config.lock();
        try {
            return config.isLogBoot();
        } finally {
            config.unlock();
        }
    }

    public void setLogBoot(boolean logBoot) {
        config.lock();
        try {
            config.setLogBoot(logBoot);
            if (logBoot == false && config.isBooting() && !config.isLogBoot()) {
                queuedItems.clear();
            }
        } finally {
            config.unlock();
        }
    }

    public Status getLoggerStatus() {
        config.lock();
        try {
            return config.getLoggerStatus();
        } finally {
            config.unlock();
        }
    }


    @Override
    public void recycleHandler(String name) {
        config.lock();
        try {
            config.recycleHandler(name);
        } finally {
            config.unlock();
        }
    }


    @Override
    public void setLoggerStatus(final Status newStatus) {
        config.lock();

        try {
            if (newStatus == Status.DISABLE_NEXT && config.getLoggerStatus() == Status.DISABLED) {
                return;
            }
            config.setLoggerStatus(newStatus);
            if (newStatus == Status.LOGGING){
                for (AuditLogItem record : queuedItems) {
                    try {
                        writeLogItem(record);
                    } catch (Exception e) {
                        handleLoggingException(e);
                    }
                }
            } else if (newStatus == Status.DISABLED){
                queuedItems.clear();
            }
        } finally {
            config.unlock();
        }
    }


    /** protected by config's audit lock */
    private void storeLogItem(AuditLogItem item) throws IOException {
        switch (getLoggerStatus()) {
            case QUEUEING:
                queuedItems.add(item);
                break;
            case LOGGING:
                writeLogItem(item);
                break;
            case DISABLE_NEXT:
                writeLogItem(item);
                config.setLoggerStatus(Status.DISABLED);
            case DISABLED:
                // ignore
                break;
        }
    }

    /** protected by config's audit lock */
    private void writeLogItem(AuditLogItem item) throws IOException{
        Set<String> formatterNames = new HashSet<String>();
        try {
            for (AuditLogHandler handler : config.getHandlersForLogging()) {
                formatterNames.add(handler.getFormatterName());
                handler.writeLogItem(item);
            }
        } finally {
            for (String formatterName : formatterNames) {
                config.getFormatter(formatterName).clear();
            }
        }
    }

    /** protected by config's audit lock */
    private void handleLoggingException(final Exception e) {
        ControllerLogger.MGMT_OP_LOGGER.failedToUpdateAuditLog(e);
        if (++failureCount == MAX_FAILURE_COUNT) {
            // Continuous failure likely indicates some configuration problem
            setLoggerStatus(Status.DISABLED);
            ControllerLogger.MGMT_OP_LOGGER.disablingLoggingDueToFailures(failureCount);
        }
    }

    public AuditLogHandlerUpdater getUpdater() {
        return this;
    }

    @Override
    public void addHandler(AuditLogHandler handler) {
        config.lock();
        try {
            if (handlerUpdateTask == null){
                handlerUpdateTask = new HandlerUpdateTask();
            }
            handlerUpdateTask.addHandler(handler);
        } finally {
            config.unlock();
        }
    }

    @Override
    public void updateHandler(AuditLogHandler handler) {
        config.lock();
        try {
            AuditLogHandler existing = config.getConfiguredHandler(handler.getName());
            if (!handler.isDifferent(existing)){
                if (handlerUpdateTask == null){
                    handlerUpdateTask = new HandlerUpdateTask();
                }
                handlerUpdateTask.replaceHandler(handler);
            }
        } finally {
            config.unlock();
        }
    }

    @Override
    public void removeHandler(String name) {
        config.lock();
        try {
            if (handlerUpdateTask == null){
                handlerUpdateTask = new HandlerUpdateTask();
            }
            handlerUpdateTask.removeHandler(name);
        } finally {
            config.unlock();
        }
    }

    @Override
    public void addHandlerReference(PathAddress referenceAddress) {
        config.lock();
        try {
            if (handlerUpdateTask == null){
                handlerUpdateTask = new HandlerUpdateTask();
            }
            handlerUpdateTask.addHandlerReference(referenceAddress);

        } finally {
            config.unlock();
        }
    }

    @Override
    public void removeHandlerReference(PathAddress referenceAddress) {
        config.lock();
        try {
            if (handlerUpdateTask == null){
                handlerUpdateTask = new HandlerUpdateTask();
            }
            handlerUpdateTask.removeHandlerReference(referenceAddress);
        } finally {
            config.unlock();
        }
    }

    @Override
    public void rollbackChanges() {
        config.lock();
        try {
            if (handlerUpdateTask != null){
                handlerUpdateTask.rollbackChanges();
                handlerUpdateTask = null;
            }
        } finally {
            config.unlock();
        }
    }

    @Override
    public void applyChanges() {
        config.lock();
        try {
            if (!config.isManualCommit()) {
                //i18n not needed, normal users will never end up here
                throw new IllegalStateException("Attempt was made to manually apply changes when manual commit was not configured");
            }
            applyHandlerUpdates();
        } finally {
            config.unlock();
        }
    }

    /** Call with lock taken */
    private void applyHandlerUpdates() {
        if (handlerUpdateTask != null) {
            handlerUpdateTask.applyChanges();
            handlerUpdateTask = null;
        }
    }

    @Override
    public void removeFormatter(String name) {
        config.lock();
        try {
            config.removeFormatter(name);
        } finally {
            config.unlock();
        }
    }

    @Override
    public void addFormatter(AuditLogItemFormatter formatter) {
        config.lock();
        try {
            config.addFormatter(formatter);
        } finally {
            config.unlock();
        }
    }


    @Override
    public void updateHandlerFormatter(String name, String formatterName) {
        config.lock();
        try {
            AuditLogHandler handler = config.getConfiguredHandler(name);
            handler.setFormatterName(formatterName);
            handler.setFormatter(config.getFormatter(formatterName));
        } finally {
            config.unlock();
        }
    }



    @Override
    public void updateHandlerMaxFailureCount(String name, int count) {
        config.lock();
        try {
            AuditLogHandler handler = config.getConfiguredHandler(name);
            handler.setMaxFailureCount(count);
        } finally {
            config.unlock();
        }
    }


    @Override
    public int getHandlerFailureCount(String name) {
        config.lock();
        try {
            AuditLogHandler handler = config.getConfiguredHandler(name);
            return handler.getFailureCount();
        } finally {
            config.unlock();
        }
    }

    @Override
    public boolean getHandlerDisabledDueToFailure(String name) {
        config.lock();
        try {
            AuditLogHandler handler = config.getConfiguredHandler(name);
            return handler.isDisabledDueToFailures();
        } finally {
            config.unlock();
        }
    }


    @Override
    public JsonAuditLogItemFormatter getJsonFormatter(String name) {
        config.lock();
        try {
            return (JsonAuditLogItemFormatter)config.getFormatter(name);
        } finally {
            config.unlock();
        }
    }


    /**
     * Abstract base class for core and new configuration
     *
     */
    abstract static class ManagedAuditLogConfiguration {

        final boolean manualCommit;
        final boolean core;
        final SharedConfiguration sharedConfiguration;

        private volatile Status status = Status.QUEUEING;
        private volatile boolean logBoot = true;
        private volatile boolean logReadOnly;


        /** Guarded by auditLock - the configured handlers we should use */
        protected final Set<String> handlerReferences = Collections.synchronizedSet(new HashSet<String>());

        ManagedAuditLogConfiguration(SharedConfiguration sharedConfiguration, boolean core, boolean manualCommit) {
            this.sharedConfiguration = sharedConfiguration;
            this.core = core;
            this.manualCommit = manualCommit;
        }

        boolean isManualCommit() {
            return manualCommit;
        }

        boolean hasHandlerReference(String name) {
            return handlerReferences.contains(name);
        }

        void addHandlerReference(String name) {
            handlerReferences.add(name);
        }

        void removeHandlerReference(String name) {
            handlerReferences.remove(name);
        }

        List<AuditLogHandler> getHandlersForLogging(){
            List<AuditLogHandler> list = new ArrayList<AuditLogHandler>();
            for (Map.Entry<String, AuditLogHandler> handlerEntry : sharedConfiguration.getConfiguredHandlers().entrySet()) {
                if (!handlerEntry.getValue().isDisabledDueToFailures() && hasHandlerReference(handlerEntry.getKey())) {
                    list.add(handlerEntry.getValue());
                }
            }
            return list;
        }

        void lock() {
            sharedConfiguration.lock();
        }

        void unlock() {
            sharedConfiguration.unlock();
        }

        String getAsVersion() {
            return sharedConfiguration.getAsVersion();
        }

        boolean isServer() {
            return sharedConfiguration.isServer();
        }

        /** Call with lock taken */
        AuditLogItemFormatter getFormatter(String name) {
            return sharedConfiguration.getFormatter(name);
        }

        /** Call with lock taken */
        AuditLogHandler getConfiguredHandler(String name) {
            return sharedConfiguration.getConfiguredHandler(name);
        }

        /** Call with lock taken */
        void setBooting(boolean booting) {
            sharedConfiguration.setBooting(booting);
        }

        /** Call with lock taken */
        boolean isBooting() {
            return sharedConfiguration.isBooting();
        }

        /** Call with lock taken */
        boolean isLogReadOnly() {
            return logReadOnly;
        }

        /** Call with lock taken */
        void setLogReadOnly(boolean logReadOnly) {
            this.logReadOnly = logReadOnly;
        }

        /** Call with lock taken */
        boolean isLogBoot() {
            return logBoot;
        }

        /** Call with lock taken */
        void setLogBoot(boolean logBoot) {
            this.logBoot = logBoot;
        }

        /** Call with lock taken */
        Status getLoggerStatus() {
            return status;
        }

        /** Call with lock taken */
        void setLoggerStatus(final Status newStatus) {
            this.status = newStatus;
        }

        boolean isCore() {
            return core;
        }

        abstract void putConfiguredHandler(AuditLogHandler handler);

        abstract AuditLogHandler removeConfiguredHandler(String name);

        abstract void addFormatter(AuditLogItemFormatter formatter);

        abstract void removeFormatter(String name);

        abstract void recycleHandler(String name);
    }

    /**
     * The configuration for the core audit logger
     */
    private static class CoreAuditLogConfiguration extends ManagedAuditLogConfiguration {

        CoreAuditLogConfiguration(String asVersion, boolean server) {
            super(new SharedConfiguration(asVersion, server), true, false);
        }

        @Override
        void putConfiguredHandler(AuditLogHandler handler) {
            sharedConfiguration.putConfiguredHandler(handler);
        }

        @Override
        AuditLogHandler removeConfiguredHandler(String name) {
            return sharedConfiguration.removeConfiguredHandler(name);
        }

        void addFormatter(AuditLogItemFormatter formatter) {
            sharedConfiguration.addFormatter(formatter);
        }

        void removeFormatter(String name) {
            sharedConfiguration.removeFormatter(name);
        }

        void recycleHandler(String name) {
            sharedConfiguration.recycleHandler(name);
        }
    }

    /**
     * The configuration for other audit loggers, e.g. jmx
     */
    private static class NewAuditLogConfiguration  extends ManagedAuditLogConfiguration {

        NewAuditLogConfiguration(CoreAuditLogConfiguration core, boolean manualCommit) {
            super(core.sharedConfiguration, false, manualCommit);
        }

        @Override
        void putConfiguredHandler(AuditLogHandler handler) {
            //i18n not needed, this will not be called by user code
            throw new IllegalStateException("Only available in core configuration");
        }

        @Override
        AuditLogHandler removeConfiguredHandler(String name) {
            //i18n not needed, this will not be called by user code
            throw new IllegalStateException("Only available in core configuration");
        }

        @Override
        void addFormatter(AuditLogItemFormatter formatter) {
            //i18n not needed, this will not be called by user code
            throw new IllegalStateException("Only available in core configuration");
        }

        @Override
        void removeFormatter(String name) {
            //i18n not needed, this will not be called by user code
            throw new IllegalStateException("Only available in core configuration");
        }

        @Override
        void recycleHandler(String name) {
            //i18n not needed, this will not be called by user code
            throw new IllegalStateException("Only available in core configuration");
        }
    }

    /**
     * Configuration shared among all the configurations and all access to methods will take place with the lock taken.
     */
    private static class SharedConfiguration {
        /** Should be fair to maintain order. Shared among all configurations */
        private final Lock auditLock = new ReentrantLock(true);
        private final String asVersion;
        private final boolean server;

        /** Guarded by auditLock - the formatters configured in the global json-formatters section */
        private final Map<String, AuditLogItemFormatter> formatters = new HashMap<String, AuditLogItemFormatter>();

        /** Guarded by auditLock - the handlers configured in the global file-handlers and syslog-handlers section */
        private final Map<String, AuditLogHandler> configuredHandlers = new HashMap<String, AuditLogHandler>();

        /** Guarded by auditLock - whether we are boothing or not */
        private boolean booting = true;


        SharedConfiguration(String asVersion, boolean server) {
            this.asVersion = asVersion;
            this.server = server;
        }

        public void recycleHandler(String name) {
            AuditLogHandler handler = configuredHandlers.get(name);
            handler.recycle();
        }

        void lock() {
            auditLock.lock();
        }

        void unlock() {
            auditLock.unlock();
        }

        String getAsVersion() {
            return asVersion;
        }

        boolean isServer() {
            return server;
        }

        Map<String, AuditLogHandler> getConfiguredHandlers() {
            return configuredHandlers;
        }

        AuditLogItemFormatter getFormatter(String name) {
            return formatters.get(name);
        }

        void addFormatter(AuditLogItemFormatter formatter) {
            formatters.put(formatter.getName(), formatter);
        }

        void removeFormatter(String name) {
            formatters.remove(name);
        }

        AuditLogHandler getConfiguredHandler(String name) {
            return configuredHandlers.get(name);
        }

        void putConfiguredHandler(AuditLogHandler handler) {
            configuredHandlers.put(handler.getName(), handler);
        }

        AuditLogHandler removeConfiguredHandler(String name) {
            return configuredHandlers.remove(name);
        }

        void setBooting(boolean booting) {
            this.booting = booting;
        }

        boolean isBooting() {
            return booting;
        }

    }


    /**
     * When we add an handler(reference) we want that to be part of the current write.
     * If we remove/change and handler, and or reference, we don't want that to take effect until the next write.
     */
    private class HandlerUpdateTask {
        private Map<String, AuditLogHandler> addedHandlers;
        private Map<String, AuditLogHandler> replacedHandlers;
        private Set<String> removedHandlers;
        private Set<PathAddress> addedReferences;
        private Set<PathAddress> removedReferences;
        private Map<String, String> updatedFormatters;

        void addHandler(AuditLogHandler handler){
            assert config.isCore() : "Not available for non-core configuration";

            if (removedHandlers != null && removedHandlers.contains(handler.getName())) {
                throw ControllerMessages.MESSAGES.attemptToBothRemoveAndAddHandlerUpdateInstead();
            }
            if (addedHandlers == null){
                addedHandlers = new HashMap<String, AuditLogHandler>();
            }
            addedHandlers.put(handler.getName(), handler);

            //Update the 'live' handlers with the addition
            config.putConfiguredHandler(handler);
            handler.setFormatter(config.getFormatter(handler.getFormatterName()));
        }

        void replaceHandler(AuditLogHandler handler){
            assert config.isCore() : "Not available for non-core configuration";

            if (addedHandlers != null && addedHandlers.containsKey(handler.getName())){
                //We may have added say a syslog-handler, and end up here as part of adding its protocol in a separate step
                addHandler(handler);
                return;
            }
            if (replacedHandlers == null){
                replacedHandlers = new HashMap<String, AuditLogHandler>();
            }

            replacedHandlers.put(handler.getName(), handler);
        }

        void removeHandler(String name) {
            assert config.isCore() : "Not available for non-core configuration";

            if (addedHandlers != null && addedHandlers.containsKey(name)){
                throw ControllerMessages.MESSAGES.attemptToBothAddAndRemoveAndHandlerFromCompositeOperation();
            }
            if (replacedHandlers != null && replacedHandlers.containsKey(name)){
                throw ControllerMessages.MESSAGES.attemptToBothUpdateAndRemoveHandlerFromCompositeOperation();
            }
            final AuditLogHandler handler = config.getConfiguredHandler(name);
            if (handler != null){
                Set<PathAddress> references = handler.getReferences();
                if (references.size() > 0){
                    if (!references.containsAll(removedReferences)){
                        Set<PathAddress> activeReferences = new HashSet<PathAddress>(references);
                        activeReferences.removeAll(removedReferences);
                        throw ControllerMessages.MESSAGES.handlerIsReferencedBy(removedReferences);
                    }
                }
            }
            config.removeConfiguredHandler(name);
            if (removedHandlers == null){
                removedHandlers = new HashSet<String>();
            }
            removedHandlers.add(name);
        }

        void addHandlerReference(PathAddress referenceAddress){
            if (removedReferences != null && removedReferences.contains(referenceAddress)){
                throw ControllerMessages.MESSAGES.attemptToBothRemoveAndAddHandlerReferenceFromCompositeOperation();
            }
            if (addedReferences == null){
                addedReferences = new HashSet<PathAddress>();
            }
            addedReferences.add(referenceAddress);

            //Update the 'live' handlers and references with the addition
            config.addHandlerReference(org.jboss.as.controller.operations.common.Util.getNameFromAddress(referenceAddress));
            final String name = org.jboss.as.controller.operations.common.Util.getNameFromAddress(referenceAddress);
            final AuditLogHandler handler = config.getConfiguredHandler(name);
            if (handler == null){
                throw ControllerMessages.MESSAGES.noHandlerCalled(name);
            }
            handler.addReference(referenceAddress);
        }

        void removeHandlerReference(PathAddress address){
            if (addedReferences != null && addedReferences.contains(address)){
                throw ControllerMessages.MESSAGES.attemptToBothRemoveAndAddHandlerReferenceFromCompositeOperation();
            }
            if (removedReferences == null){
                removedReferences = new HashSet<PathAddress>();
            }
            removedReferences.add(address);
        }

        void rollbackChanges(){
            if (addedReferences != null && addedReferences.size() > 0){
                for (PathAddress address : addedReferences) {
                    final String name = org.jboss.as.controller.operations.common.Util.getNameFromAddress(address);
                    config.removeHandlerReference(name);
                    AuditLogHandler handler = config.getConfiguredHandler(name);
                    if (handler != null){
                        handler.removeReference(address);
                    }
                }
            }
            if (addedHandlers != null && addedHandlers.size() > 0){
                for (AuditLogHandler handler : addedHandlers.values()){
                    config.removeConfiguredHandler(handler.getName());
                }
            }
        }

        void applyChanges() {
            if (removedHandlers != null && removedHandlers.size() > 0){
                for (String name : removedHandlers) {
                    AuditLogHandler handler = config.removeConfiguredHandler(name);
                    if (handler != null){
                        handler.stop();
                    }
                }
            }
            if (replacedHandlers != null && replacedHandlers.size() > 0){
                for (AuditLogHandler handler : replacedHandlers.values()) {
                    AuditLogHandler existing = config.removeConfiguredHandler(handler.getName());
                    if (existing != null){
                        existing.stop();
                    }
                    //Update the references for the replaced one
                    for (PathAddress referenceAddress : existing.getReferences()){
                        if (removedReferences != null && !removedReferences.contains(referenceAddress)){
                            handler.addReference(referenceAddress);
                        }
                    }
                    config.putConfiguredHandler(handler);
                    handler.setFormatter(config.getFormatter(handler.getFormatterName()));
                }
            }
            if (removedReferences != null && removedReferences.size() > 0){
                for (PathAddress referenceAddress : removedReferences){
                    final String name = org.jboss.as.controller.operations.common.Util.getNameFromAddress(referenceAddress);
                    final AuditLogHandler handler = config.getConfiguredHandler(name);
                    if (handler != null){
                        handler.removeReference(referenceAddress);
                    }
                    config.removeHandlerReference(name);
                }
            }
        }
    }


    @Override
    public void bootDone() {
        config.lock();
        try {
            config.setBooting(false);
            if (childImpls != null) {
                for (ManagedAuditLogger child : childImpls) {
                    child.bootDone();
                }
            }
            if (config.getLoggerStatus() == Status.QUEUEING) {
                //There was no configuration to either enable or disable the logger during boot, so disable it
                setLoggerStatus(AuditLogger.Status.DISABLED);
            }
        } finally {
            config.unlock();
        }
    }


    @Override
    public void startBoot() {
        config.lock();
        try {
            config.setBooting(true);
            if (childImpls != null) {
                childImpls.clear();
            }
            config.setLoggerStatus(Status.QUEUEING);
        } finally {
            config.unlock();
        }
    }

 }