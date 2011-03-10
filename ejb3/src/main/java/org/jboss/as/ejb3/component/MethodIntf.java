/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

/**
 * The method-intf element allows a method element to
 * differentiate between the methods with the same name and
 * signature that are multiply defined across the home and
 * component interfaces (e.g, in both an enterprise bean's
 * remote and local interfaces or in both an enterprise bean's
 * home and remote interfaces, etc.); the component and web
 * service endpoint interfaces, and so on.
 * <p/>
 * Local applies to the local component interface, local business
 * interfaces, and the no-interface view.
 * <p/>
 * Remote applies to both remote component interface and the remote
 * business interfaces.
 * <p/>
 * ServiceEndpoint refers to methods exposed through a web service
 * endpoint.
 * <p/>
 * Timer refers to the bean's timeout callback methods.
 * <p/>
 * MessageEndpoint refers to the methods of a message-driven bean's
 * message-listener interface.
 * <p/>
 * The method-intf element must be one of the following:
 *
 * <ul>
 * <li>Home</li>
 * <li>Remote</li>
 * <li>LocalHome</li>
 * <li>Local</li>
 * <li>ServiceEndpoint</li>
 * <li>Timer</li>
 * <li>MessageEndpoint</li>
 * </ul>
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public enum MethodIntf {
    BEAN, // represents a direct call to the EJB (temporary)
    HOME,
    REMOTE,
    LOCAL_HOME,
    LOCAL,
    SERVICE_ENDPOINT,
    TIMER,
    MESSAGE_ENDPOINT,
}
