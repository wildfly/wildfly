/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import java.io.Serializable;

import org.wildfly.clustering.server.dispatcher.Command;

import jakarta.resource.spi.work.WorkException;

/**
 * A transport command.
 * @author Paul Ferraro
 */
public interface TransportCommand<R> extends Command<R, CommandDispatcherTransport, WorkException>, Serializable {

}
