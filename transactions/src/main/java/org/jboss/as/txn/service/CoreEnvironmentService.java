/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.utils.Process;
import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * An msc service for setting up the
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class CoreEnvironmentService implements Service {

    private final Consumer<CoreEnvironmentBean> coreEnvironmentBeanConsumer;
    /** A dependency on a socket binding for the socket process id */
    private final Supplier<SocketBinding>  socketBindingSupplier;
    private final Supplier<String> nodeIdentifierSupplier;
    private final Supplier<Integer> portsSupplier;
    private final Supplier<String> processImplClassSupplier;
    private final Supplier<Process> processImplSupplier;

    public CoreEnvironmentService(final Consumer<CoreEnvironmentBean> coreEnvironmentBeanConsumer, final Supplier<SocketBinding>  socketBindingSupplier,
                                  final Supplier<String> nodeIdentifierSupplier, final Supplier<Integer> portsSupplier,
                                  final Supplier<String> processImplClassSupplier, final Supplier<Process> processImplSupplier) {
        this.coreEnvironmentBeanConsumer = coreEnvironmentBeanConsumer;
        this.socketBindingSupplier = socketBindingSupplier;
        this.nodeIdentifierSupplier = nodeIdentifierSupplier;
        this.portsSupplier = portsSupplier;
        this.processImplClassSupplier = processImplClassSupplier;
        this.processImplSupplier = processImplSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {

        // Global configuration.
        final CoreEnvironmentBean coreEnvironmentBean = arjPropertyManager.getCoreEnvironmentBean();
        if (portsSupplier != null) coreEnvironmentBean.setSocketProcessIdPort(portsSupplier.get());
        if (processImplClassSupplier != null) coreEnvironmentBean.setProcessImplementationClassName(processImplClassSupplier.get());
        if (processImplSupplier != null) coreEnvironmentBean.setProcessImplementation(processImplSupplier.get());

        if (coreEnvironmentBean.getProcessImplementationClassName() == null) {
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentBean.setProcessImplementation(id);
        }

        try {
            coreEnvironmentBean.setNodeIdentifier(nodeIdentifierSupplier.get());
        } catch (CoreEnvironmentBeanException e) {
            throw new StartException(e.getCause());
        }

        // Setup the socket process id if there is a binding
        SocketBinding binding = socketBindingSupplier.get();
        if(binding != null) {
            int port = binding.getPort();
            coreEnvironmentBean.setSocketProcessIdPort(port);
        }

        coreEnvironmentBeanConsumer.accept(coreEnvironmentBean);
    }

    @Override
    public void stop(final StopContext context) {
        coreEnvironmentBeanConsumer.accept(null);
    }
}
