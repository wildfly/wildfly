/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.function.Supplier;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * @author Emanuel Muckenhuber
 */
public class GroupBindingService implements Service<SocketBinding> {

    private static final String BASE = "bindings";
    private static final String BROADCAST = "broadcast";
    private static final String DISCOVERY = "discovery";

    private final Supplier<SocketBinding> bindingSupplier;

    public GroupBindingService(Supplier<SocketBinding> bindingSupplier) {
        this.bindingSupplier = bindingSupplier;
    }
    @Override
    public void start(final StartContext context) throws StartException {
        //
    }

    @Override
    public void stop(StopContext context) {
        //
    }

    @Override
    public SocketBinding getValue() throws IllegalStateException, IllegalArgumentException {
        return bindingSupplier.get();
    }

    public static ServiceName getBroadcastBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(BASE).append(BROADCAST);
    }

    public static ServiceName getDiscoveryBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(BASE).append(DISCOVERY);
    }
}
