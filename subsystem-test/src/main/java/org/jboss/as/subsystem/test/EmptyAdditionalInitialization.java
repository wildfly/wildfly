/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.msc.service.ServiceTarget;

/**
 * Empty implmentation of AdditionalInitialization so that you only need to override the methods you actually need.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class EmptyAdditionalInitialization implements AdditionalInitialization {

    @Override
    public void addParsers(ExtensionParsingContext context) {
    }

    @Override
    public void setupController(ControllerInitializer controllerInitializer) {
    }

    @Override
    public void addExtraServices(ServiceTarget target) {
    }

    @Override
    public void initializeExtraSubystemsAndModel(ExtensionContext context, Resource rootResource,
            ManagementResourceRegistration rootRegistration) {
    }

}
