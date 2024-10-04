/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * The interface for a Jakarta Concurrency Implementation
 * @author Eduardo Martins
 */
public interface ConcurrencyImplementation {

    ConcurrencyImplementation INSTANCE = ConcurrencyImplementationLoader.load();

    // extension init ops
    void registerRootResourceSubModels(ManagementResourceRegistration rootResource, ExtensionContext context);

    // subsystem add ops
    void addDeploymentProcessors(DeploymentProcessorTarget processorTarget);
    void installSubsystemServices(OperationContext context);

    // XML config ops
    void parseConcurrentElement20(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement40(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement50(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement60(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException;
}
