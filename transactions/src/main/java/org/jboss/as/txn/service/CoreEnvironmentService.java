/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.service;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.utils.Process;
import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;

import java.util.function.Supplier;

/**
 * An msc service for setting up the
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @version $Revision:$
 */
public class CoreEnvironmentService implements Service<CoreEnvironmentBean> {

    /** A dependency on a socket binding for the socket process id */
    private final Supplier<SocketBinding>  socketBindingSupplier;
    private final String nodeIdentifier;

    public CoreEnvironmentService(final String nodeIdentifier, final Supplier<SocketBinding>  socketBindingSupplier) {
        this.nodeIdentifier = nodeIdentifier;
        this.socketBindingSupplier = socketBindingSupplier;
    }

    @Override
    public CoreEnvironmentBean getValue() throws IllegalStateException, IllegalArgumentException {
        CoreEnvironmentBean coreEnvironmentBean = arjPropertyManager.getCoreEnvironmentBean();
        return coreEnvironmentBean;
    }

    @Override
    public void start(StartContext context) throws StartException {

        // Global configuration.
        final CoreEnvironmentBean coreEnvironmentBean = arjPropertyManager.getCoreEnvironmentBean();
        if(coreEnvironmentBean.getProcessImplementationClassName() == null) {
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentBean.setProcessImplementation(id);
        }

        try {
            coreEnvironmentBean.setNodeIdentifier(nodeIdentifier);
        } catch (CoreEnvironmentBeanException e) {
            throw new StartException(e.getCause());
        }

        // Setup the socket process id if there is a binding
        SocketBinding binding = socketBindingSupplier.get();
        if(binding != null) {
            int port = binding.getPort();
            coreEnvironmentBean.setSocketProcessIdPort(port);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

    public int getSocketProcessIdMaxPorts() {
        return getValue().getSocketProcessIdMaxPorts();
    }
    public void setSocketProcessIdMaxPorts(int socketProcessIdMaxPorts) {
        getValue().setSocketProcessIdMaxPorts(socketProcessIdMaxPorts);
    }

    public void setProcessImplementationClassName(String clazz) {
        getValue().setProcessImplementationClassName(clazz);
    }
    public void setProcessImplementation(Process instance) {
        getValue().setProcessImplementation(instance);
    }
}
