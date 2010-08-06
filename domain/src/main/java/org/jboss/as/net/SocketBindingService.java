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
package org.jboss.as.net;

import org.jboss.as.model.socket.SocketBindingElement;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingService implements Service<SocketBinding> {

	private final SocketBindingElement element;
	private final InjectedValue<NetworkInterfaceBinding> interfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
	private final InjectedValue<SocketBindingManager> socketBindings = new InjectedValue<SocketBindingManager>();
	
	private SocketBinding binding;
	
	public SocketBindingService(final SocketBindingElement element) {
		this.element = element;
	}
	
	public synchronized void start(StartContext arg0) throws StartException {
		this.binding = new SocketBinding(element, interfaceBinding.getValue(), socketBindings.getValue());
	}

	public synchronized void stop(StopContext arg0) {
		this.binding = null;
	}

	public synchronized SocketBinding getValue() throws IllegalStateException {
		final SocketBinding binding = this.binding;
		if(binding == null) {
			throw new IllegalStateException();
		}
		return binding;
	}

	public InjectedValue<SocketBindingManager> getSocketBindings() {
		return socketBindings;
	}
	
	public InjectedValue<NetworkInterfaceBinding> getInterfaceBinding() {
		return interfaceBinding;
	}
}

