/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.dmr.ModelNode;
import org.xnio.XnioWorker;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class AccessLogAdd extends AbstractAddStepHandler {

    private AccessLogAdd() {
        super(AccessLogDefinition.ATTRIBUTES);
    }

    static final AccessLogAdd INSTANCE = new AccessLogAdd();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress hostAddress = address.getParent();
        final PathAddress serverAddress = hostAddress.getParent();
        final String worker = AccessLogDefinition.WORKER.resolveModelAttribute(context, model).asString();
        final String pattern = AccessLogDefinition.PATTERN.resolveModelAttribute(context, model).asString();
        final String directory = AccessLogDefinition.DIRECTORY.resolveModelAttribute(context, model).asString();
        final String filePrefix = AccessLogDefinition.PREFIX.resolveModelAttribute(context, model).asString();
        final String fileSuffix = AccessLogDefinition.SUFFIX.resolveModelAttribute(context, model).asString();
        final boolean useServerLog = AccessLogDefinition.USE_SERVER_LOG.resolveModelAttribute(context, model).asBoolean();
        final boolean rotate = AccessLogDefinition.ROTATE.resolveModelAttribute(context, model).asBoolean();
        final boolean extended = AccessLogDefinition.EXTENDED.resolveModelAttribute(context, model).asBoolean();
        final ModelNode relativeToNode = AccessLogDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;
        final int closeRetryDelay = AccessLogDefinition.CLOSE_RETRY_DELAY.resolveModelAttribute(context, model).asInt();
        final int closeRetryCount = AccessLogDefinition.CLOSE_RETRY_COUNT.resolveModelAttribute(context, model).asInt();

        Predicate predicate = null;
        ModelNode predicateNode = AccessLogDefinition.PREDICATE.resolveModelAttribute(context, model);
        if(predicateNode.isDefined()) {
            predicate = Predicates.parse(predicateNode.asString(), getClass().getClassLoader());
        }

        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();

        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(AccessLogDefinition.ACCESS_LOG_CAPABILITY);
        final Consumer<AccessLogService> sConsumer = sb.provides(AccessLogDefinition.ACCESS_LOG_CAPABILITY, UndertowService.accessLogServiceName(serverName, hostName));
        final Supplier<Host> hSupplier = sb.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName);
        final Supplier<XnioWorker> wSupplier = sb.requiresCapability(Capabilities.REF_IO_WORKER, XnioWorker.class, worker);
        final Supplier<PathManager> pmSupplier = sb.requires(PathManagerService.SERVICE_NAME);
        final AccessLogService service;
        if (useServerLog) {
            service = new AccessLogService(sConsumer, hSupplier, wSupplier, pmSupplier, pattern, extended, predicate);
        } else {
            service = new AccessLogService(sConsumer, hSupplier, wSupplier, pmSupplier, pattern, directory, relativeTo, filePrefix, fileSuffix, rotate, extended, false, predicate, closeRetryCount, closeRetryDelay);
        }
        sb.setInstance(service);
        sb.install();
    }
}
