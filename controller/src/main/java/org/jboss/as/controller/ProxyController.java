/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller;


/**
 * A proxy controller to be registered with a ModelController.
 * <p/>
 * Proxy controllers apply to a given address in the host ModelController
 * and typically allow access to an external ModelController.
 * <p/>
 * For example if a ProxyController is registered in the host ModelController for the address
 * <code>[a=b,c=d]</code>, then an operation executed in the host ModelController for
 * <code>[a=b]</code> will execute in the host model controller as normal. An operation for
 * <code>[a=b,c=d,x=y]</code> will apply to <code>[x=y]</code> in the model controller
 * pointed to by this proxy controller.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ProxyController extends ModelController {

    /**
     * Get the address where this proxy controller applies to in the host ModelController
     *
     * @return the address where this proxy contoller applies.
     */
    PathAddress getProxyNodeAddress();

}