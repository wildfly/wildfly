/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.insights.extension;


import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.JBossThreadFactory;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.insights.extension.InsightsSubsystemDefinition.ATTRIBUTES;
import static org.jboss.as.insights.extension.InsightsSubsystemDefinition.INSIGHTS_RUNTIME_CAPABILITY;

import org.jboss.as.controller.AttributeDefinition;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jkinlaw@redhat.com">Josh Kinlaw</a>
 */
class InsightsSubsystemAdd extends AbstractAddStepHandler {

    static final InsightsSubsystemAdd INSTANCE = new InsightsSubsystemAdd();

    private InsightsSubsystemAdd() {
        super(INSIGHTS_RUNTIME_CAPABILITY, ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        boolean enabled = InsightsSubsystemDefinition.ENABLED.resolveModelAttribute(context, operation).asBoolean();
        InsightsConfiguration config = new InsightsConfiguration(
                getStringValue(InsightsSubsystemDefinition.INSIGHTSENDPOINT, context, operation),
                getStringValue(InsightsSubsystemDefinition.SYSTEMENDPOINT, context, operation),
                getStringValue(InsightsSubsystemDefinition.RHNUID, context, operation),
                getStringValue(InsightsSubsystemDefinition.RHNPW, context, operation),
                getStringValue(InsightsSubsystemDefinition.PROXYURL, context, operation),
                InsightsSubsystemDefinition.PROXYPORT.resolveModelAttribute(context, operation).asInt(),
                getStringValue(InsightsSubsystemDefinition.PROXYUSER, context, operation),
                getStringValue(InsightsSubsystemDefinition.PROXYPASSWORD, context, operation),
                getStringValue(InsightsSubsystemDefinition.URL, context, operation),
                getStringValue(InsightsSubsystemDefinition.USERAGENT, context, operation),
                InsightsSubsystemDefinition.SCHEDULE_INTERVAL.resolveModelAttribute(context, operation).asInt());
        InsightsService.addService(context.getServiceTarget(), createUploaderExecutorService(), enabled, config);
    }

    private String getStringValue(AttributeDefinition def, OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode result = def.resolveModelAttribute(context, operation);
        if(result.isDefined()) {
            return result.asString();
        }
        return null;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    static ScheduledExecutorService createUploaderExecutorService() {
        final ThreadFactory threadFactory = doPrivileged(
                (PrivilegedAction<ThreadFactory>) () -> new JBossThreadFactory(
                        new ThreadGroup("InsightsUpload-threads"), Boolean.FALSE, null, "%G - %t", null, null));
        return Executors.newScheduledThreadPool(1, threadFactory);
    }
}
