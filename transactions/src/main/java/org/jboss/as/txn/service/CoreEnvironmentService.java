/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.txn.service;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.utils.Process;
import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;

/**
 * An msc service for setting up the
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @version $Revision:$
 */
public class CoreEnvironmentService implements Service<CoreEnvironmentBean> {

    /** A dependency on a socket binding for the socket process id */
    private final InjectedValue<SocketBinding> socketProcessBindingInjector = new InjectedValue<SocketBinding>();
    private final String nodeIdentifier;

    public CoreEnvironmentService(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
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
        SocketBinding binding = socketProcessBindingInjector.getOptionalValue();
        if(binding != null) {
            int port = binding.getPort();
            coreEnvironmentBean.setSocketProcessIdPort(port);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

    public Injector<SocketBinding> getSocketProcessBindingInjector() {
        return socketProcessBindingInjector;
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
