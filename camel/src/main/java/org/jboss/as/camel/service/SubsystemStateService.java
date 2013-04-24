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

package org.jboss.as.camel.service;

import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.parser.SubsystemState;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * The {@link SubsystemState} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class SubsystemStateService extends AbstractService<SubsystemState> {

    static final ServiceName SERVICE_NAME = CamelConstants.CAMEL_BASE_NAME.append("state");
    private final SubsystemState subsystemState;

    public static ServiceController<SubsystemState> addService(ServiceTarget serviceTarget, SubsystemState subsystemState, ServiceVerificationHandler verificationHandler) {
        SubsystemStateService service = new SubsystemStateService(subsystemState);
        ServiceBuilder<SubsystemState> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private SubsystemStateService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    public SubsystemState getValue() {
        return subsystemState;
    }
}
