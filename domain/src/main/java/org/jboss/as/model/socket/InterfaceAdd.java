/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.model.AbstractModelElementUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * A {@code InterfaceElement} update.
 *
 * @author Emanuel Muckenhuber
 */
public final class InterfaceAdd extends AbstractNetworkInterfaceUpdate {

    private static final long serialVersionUID = 2597565379666344003L;
    private final String name;
    private boolean anyLocalV4;
    private boolean anyLocalV6;
    private boolean anyLocal;
    private Collection<AbstractInterfaceCriteriaElement<?>> interfaceCriteria = Collections.emptySet();

    public InterfaceAdd(String name, boolean anyLocalV4, boolean anyLocalV6, boolean anyLocal, Collection<AbstractInterfaceCriteriaElement<?>> interfaceCriteria) {
        this.name = name;
        this.anyLocalV4 = anyLocalV4;
        this.anyLocalV6 = anyLocalV6;
        this.anyLocal = anyLocal;
        this.interfaceCriteria = interfaceCriteria;
    }

    public InterfaceAdd(final InterfaceElement networkInterface) {
        name = networkInterface.getName();
        anyLocal = networkInterface.isAnyLocalAddress();
        anyLocalV4 = networkInterface.isAnyLocalV4Address();
        anyLocalV6 = networkInterface.isAnyLocalV6Address();
        interfaceCriteria = networkInterface.getCriteriaElements();
    }

    public String getName() {
        return name;
    }

    public boolean isFullySpecified() {
        return anyLocal || anyLocalV4 || anyLocalV6 || (interfaceCriteria != null && interfaceCriteria.size() > 0);
    }

    /** {@inheritDoc} */
    @Override
    public void applyUpdate(InterfaceElement element) throws UpdateFailedException {
        if(interfaceCriteria != null && interfaceCriteria.size() > 0) {
            for(AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria) {
                element.addCriteria(criteria);
            }
        }
        if(anyLocal) element.setAnyLocal(anyLocal);
        if(anyLocalV4) element.setAnyLocalV4(anyLocalV4);
        if(anyLocalV6) element.setAnyLocalV6(anyLocalV6);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractModelElementUpdate<InterfaceElement> getCompensatingUpdate(InterfaceElement original) {
        return null;
    }


    public <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final BatchBuilder batch = updateContext.getServiceTarget();
        batch.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name), createInterfaceService())
            .addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param))
            .setInitialMode(Mode.ON_DEMAND)
            .install();
    }

    /**
     * Create a {@link NetworkInterfaceService}.
     *
     * @return the interface service
     */
    Service<NetworkInterfaceBinding> createInterfaceService() {
        Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>();
        for(final AbstractInterfaceCriteriaElement<?> element : interfaceCriteria) {
            criteria.add(element.getInterfaceCriteria());
        }
        return new NetworkInterfaceService(name, anyLocalV4, anyLocalV6, anyLocal, new OverallInterfaceCriteria(criteria));
    }

    /** Overall interface criteria. */
    static final class OverallInterfaceCriteria implements InterfaceCriteria {
        private static final long serialVersionUID = -5417786897309925997L;
        private final Set<InterfaceCriteria> interfaceCriteria;

        public OverallInterfaceCriteria(Set<InterfaceCriteria> criteria) {
            interfaceCriteria = criteria;
        }

        /** {@inheritDoc} */
        public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
            for (InterfaceCriteria criteria : interfaceCriteria) {
                if (! criteria.isAcceptable(networkInterface, address))
                    return false;
            }
            return true;
        }
    }

}
