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

package org.jboss.as.jdr;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.common.function.Functions;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JdrReportSubsystemDefinition extends SimpleResourceDefinition {

    static final String MCF_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String EXECUTOR_CAPABILITY = "org.wildfly.management.executor";

    static final RuntimeCapability<Void> JDR_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jdr", JdrReportCollector.class)
            //.addRequirements(MCF_CAPABILITY, EXECUTOR_CAPABILITY) TODO determine why this breaks domain mode provisioning
            .build();

    private final AtomicReference<JdrReportCollector> collectorReference;

    JdrReportSubsystemDefinition(AtomicReference<JdrReportCollector> collectorReference) {
        super(getParameters(collectorReference));
        this.collectorReference = collectorReference;
    }

    private static Parameters getParameters(AtomicReference<JdrReportCollector> collectorReference) {
        Consumer<JdrReportCollector> subsystemConsumer = collectorReference == null ? Functions.discardingConsumer() : collectorReference::set;
        OperationStepHandler addHandler = new JdrReportSubsystemAdd(subsystemConsumer);
        return new Parameters(JdrReportExtension.SUBSYSTEM_PATH, JdrReportExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(addHandler)
                .setRemoveHandler(JdrReportSubsystemRemove.INSTANCE)
                .setCapabilities(JDR_CAPABILITY);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        if (collectorReference != null) {
            resourceRegistration.registerOperationHandler(JdrReportRequestHandler.DEFINITION, new JdrReportRequestHandler(collectorReference::get));
        }
    }
}
