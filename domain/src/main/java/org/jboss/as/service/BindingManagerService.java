/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.model.Standalone;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.SocketBindingElement;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Emanuel Muckenhuber
 */
public class BindingManagerService implements Service<ServiceBindingManager> {

	public final ServiceName NAME = ServiceName.JBOSS.append("service", "ServiceBindingManager");

	private final Standalone server;
	private ServiceBindingManager manager;
	
	public BindingManagerService(Standalone model) {
		this.server = model;
	}
	
	public synchronized void start(StartContext context) throws StartException {
		final HashMap<String, InetAddress> networkAddresses = new HashMap<String, InetAddress>();
		final List<InterfaceElement> elements = new ArrayList<InterfaceElement>(server.getInterfaces());
		try {
			final Iterator<InterfaceElement> elementsIterator = elements.iterator();
			INTERFACES_LOOP: while(elementsIterator.hasNext()) {
				final InterfaceElement element = elementsIterator.next();
				final String interfaceName = element.getName();
				if(element.getAddress() != null) {
					final InetAddress address = InetAddress.getByName(element.getAddress());
					networkAddresses.put(interfaceName, address);
					elementsIterator.remove();
					continue;
				} else {
					final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
					while(networkInterfaces.hasMoreElements()) {
						final NetworkInterface networkInterface = networkInterfaces.nextElement();			
						while(elementsIterator.hasNext()) {
							final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
							while(interfaceAddresses.hasMoreElements()) {
								final InetAddress address = interfaceAddresses.nextElement();
								if(element.getInterfaceCriteria().isAcceptable(networkInterface, address)) {
									networkAddresses.put(interfaceName, address);
									elementsIterator.remove();
									continue INTERFACES_LOOP;
								}
							}
						}
					}
				}
			}
		} catch(Exception e) {
			throw new StartException(e);
		}
		if(! elements.isEmpty()) {
			throw new StartException("failed to resolved interfaces " + elements);
		}
		final HashMap<String, SocketBindingElement> bindings = new HashMap<String, SocketBindingElement>();
		for(final SocketBindingElement binding : server.getSocketBindings().getAllSocketBindings()) {
			bindings.put(binding.getName(), binding);
		}
		this.manager = new ServiceBindingManager(bindings, networkAddresses, server.getPortOffset());
	}

	public synchronized void stop(StopContext context) {
		this.manager = null;
	}

	public synchronized ServiceBindingManager getValue() throws IllegalStateException {
		final ServiceBindingManager manager = this.manager;
		if(manager == null) {
			throw new IllegalStateException();
		}
		return manager;
	}
}

