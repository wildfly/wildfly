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
package org.jboss.as.controller.audit;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.audit.spi.AuditLogEventFormatter;
import org.jboss.as.controller.audit.spi.AuditLogEventType;
import org.jboss.as.controller.audit.spi.JmxAccessAuditLogEvent;
import org.jboss.as.controller.audit.spi.ModelControllerAuditLogEvent;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class AuditLogItemImpl {

    private final Date date = new Date();
    private final String asVersion;
    protected final boolean readOnly;
    protected final boolean booting;
    protected final String userId;
    protected final String domainUUID;
    protected final AccessMechanism accessMechanism;
    protected final InetAddress remoteAddress;

    AuditLogItemImpl(String asVersion, boolean readOnly, boolean booting, String userId, String domainUUID, AccessMechanism accessMechanism,
            InetAddress remoteAddress) {
        this.asVersion = asVersion;
        this.readOnly = readOnly;
        this.booting = booting;
        this.userId = userId;
        this.domainUUID = domainUUID;
        this.accessMechanism = accessMechanism;
        this.remoteAddress = remoteAddress;
    }

    static AuditLogItemImpl createModelControllerItem(String asVersion, boolean readOnly, boolean booting, ResultAction resultAction, String userId,
                String domainUUID, AccessMechanism accessMechanism, InetAddress remoteAddress, Resource resultantModel,
                List<ModelNode> operations) {
        return new ModelControllerAuditLogItemImpl(asVersion, readOnly, booting, resultAction, userId, domainUUID, accessMechanism, remoteAddress, resultantModel, operations);
    }

    static AuditLogItemImpl createMethodAccessItem(String asVersion, boolean readOnly, boolean booting, String userId, String domainUUID,
                AccessMechanism accessMechanism, InetAddress remoteAddress, String methodName, String[] methodSignature,
                Object[] methodParams, Throwable error) {
        return new JmxAccessAuditLogItemImpl(asVersion, readOnly, booting, userId, domainUUID, accessMechanism, remoteAddress, methodName, methodSignature, methodParams, error);
    }


    abstract String format(AuditLogEventFormatter formatter);

    /**
     * Get the asVersion
     * @return the asVersion
     */
    public String getAsVersion() {
        return asVersion;
    }

    /**
     * Get the readOnly
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Get the booting
     * @return the booting
     */
    public boolean isBooting() {
        return booting;
    }

    /**
     * Get the userId
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the domainUUID
     * @return the domainUUID
     */
    public String getDomainUUID() {
        return domainUUID;
    }

    /**
     * Get the accessMechanism
     * @return the accessMechanism
     */
    public AccessMechanism getAccessMechanism() {
        return accessMechanism;
    }

    /**
     * Get the remoteAddress
     * @return the remoteAddress
     */
    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Get the date
     * @return the date
     */
    public Date getDate() {
        return (Date)date.clone();
    }


    static class ModelControllerAuditLogItemImpl extends AuditLogItemImpl implements ModelControllerAuditLogEvent {
        private final ResultAction resultAction;
        private final Resource resultantModel;
        private final List<ModelNode> operations;

        ModelControllerAuditLogItemImpl(String asVersion, boolean readOnly, boolean booting, ResultAction resultAction, String userId,
                String domainUUID, AccessMechanism accessMechanism, InetAddress remoteAddress, Resource resultantModel,
                List<ModelNode> operations) {
            super(asVersion, readOnly, booting, userId, domainUUID, accessMechanism, remoteAddress);
            this.resultAction = resultAction;
            this.resultantModel = resultantModel;
            this.operations = operations;
        }

        @Override
        String format(AuditLogEventFormatter formatter) {
            return formatter.formatAuditLogItem(this);
        }

        public AuditLogEventType getType() {
            return AuditLogEventType.CORE;
        }

        /**
         * Get the resultAction
         * @return the resultAction
         */
        public ResultAction getResultAction() {
            return resultAction;
        }

        /**
         * Get the resultantModel
         * @return the resultantModel
         */
        public Resource getResultantModel() {
            return resultantModel;
        }

        /**
         * Get the operations
         * @return the operations
         */
        public List<ModelNode> getOperations() {
            return operations;
        }
    }

    static class JmxAccessAuditLogItemImpl extends AuditLogItemImpl implements JmxAccessAuditLogEvent {
        private final String methodName;
        private final String[] methodSignature;
        private final Object[] methodParams;
        private final Throwable error;

        JmxAccessAuditLogItemImpl(String asVersion, boolean readOnly, boolean booting, String userId, String domainUUID,
                AccessMechanism accessMechanism, InetAddress remoteAddress, String methodName, String[] methodSignature,
                Object[] methodParams, Throwable error) {
            super(asVersion, readOnly, booting, userId, domainUUID, accessMechanism, remoteAddress);
            this.methodName = methodName;
            this.methodSignature = methodSignature;
            this.methodParams = methodParams;
            this.error = error;
        }

        String format(AuditLogEventFormatter formatter) {
            return formatter.formatAuditLogItem(this);
        }

        public AuditLogEventType getType() {
            return AuditLogEventType.JMX;
        }

        /**
         * Get the methodName
         * @return the methodName
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * Get the methodSignature
         * @return the methodSignature
         */
        public String[] getMethodSignature() {
            return methodSignature;
        }

        /**
         * Get the methodParams
         * @return the methodParams
         */
        public Object[] getMethodParams() {
            return methodParams;
        }

        /**
         * Get the error
         * @return the error
         */
        public Throwable getError() {
            return error;
        }
    }
}
