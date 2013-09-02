/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jmx;

import java.net.InetAddress;
import java.util.List;

import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.audit.AuditLogItemFormatter;
import org.jboss.as.controller.audit.JsonAuditLogItemFormatter;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class JmxManagedAuditLogger implements ManagedAuditLogger {
    private final ManagedAuditLogger auditLogger;
    private volatile boolean booting = true;

    public JmxManagedAuditLogger(ManagedAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    void setBooting(boolean booting) {
        this.booting = booting;
    }

    boolean shouldLog(boolean readOnly) {
        if (booting && !auditLogger.isLogBoot()) {
            return false;
        }
        if (readOnly && !auditLogger.isLogReadOnly()) {
            return false;
        }
        return true;
    }

    public boolean isLogReadOnly() {
        return auditLogger.isLogReadOnly();
    }

    public void setLogReadOnly(boolean logReadOnly) {
        auditLogger.setLogReadOnly(logReadOnly);
    }

    public boolean isLogBoot() {
        return auditLogger.isLogBoot();
    }

    public void setLogBoot(boolean logBoot) {
        auditLogger.setLogBoot(logBoot);
    }

    public Status getLoggerStatus() {
        return auditLogger.getLoggerStatus();
    }

    public void setLoggerStatus(Status newStatus) {
        auditLogger.setLoggerStatus(newStatus);
    }

    public AuditLogHandlerUpdater getUpdater() {
        return auditLogger.getUpdater();
    }

    public void recycleHandler(String name) {
        auditLogger.recycleHandler(name);
    }

    public ManagedAuditLogger createNewConfiguration(boolean manualCommit) {
        return auditLogger.createNewConfiguration(manualCommit);
    }

    public void addFormatter(AuditLogItemFormatter formatter) {
        auditLogger.addFormatter(formatter);
    }

    public void updateHandlerFormatter(String name, String formatterName) {
        auditLogger.updateHandlerFormatter(name, formatterName);
    }

    public void updateHandlerMaxFailureCount(String name, int count) {
        auditLogger.updateHandlerMaxFailureCount(name, count);
    }

    public void removeFormatter(String name) {
        auditLogger.removeFormatter(name);
    }

    public int getHandlerFailureCount(String name) {
        return auditLogger.getHandlerFailureCount(name);
    }

    public boolean getHandlerDisabledDueToFailure(String name) {
        return auditLogger.getHandlerDisabledDueToFailure(name);
    }

    public JsonAuditLogItemFormatter getJsonFormatter(String name) {
        return auditLogger.getJsonFormatter(name);
    }

    public void log(boolean readOnly, boolean booting, ResultAction resultAction, String userId, String domainUUID,
            AccessMechanism accessMechanism, InetAddress remoteAddress, Resource resultantModel, List<ModelNode> operations) {
        //i18n not needed, if we end up here it is a bug in the JMX subsystem
        throw new IllegalStateException("Not implemented");
    }

    public void logMethodAccess(boolean readOnly, boolean booting, String userId, String domainUUID,
            AccessMechanism accessMechanism, InetAddress remoteAddress, String methodName, String[] methodSignature,
            Object[] methodParams, Throwable error) {
        auditLogger.logMethodAccess(readOnly, booting, userId, domainUUID, accessMechanism, remoteAddress, methodName,
                methodSignature, methodParams, error);
    }

    void logMethodAccess(boolean readOnly, String userId, String domainUUID,
            AccessMechanism accessMechanism, InetAddress remoteAddress, String methodName, String[] methodSignature,
            Object[] methodParams, Throwable error) {
        logMethodAccess(readOnly, booting, userId, domainUUID, accessMechanism, remoteAddress, methodName, methodSignature, methodParams, error);
    }

    @Override
    public void bootDone() {
        auditLogger.bootDone();
    }
}
