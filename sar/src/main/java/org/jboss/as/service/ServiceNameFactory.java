/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import org.jboss.as.service.logging.SarLogger;
import org.jboss.msc.service.ServiceName;

/**
 * Utility class for creating mBean service names.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceNameFactory {

    private static final ServiceName MBEAN_SERVICE_NAME_BASE = ServiceName.JBOSS.append("mbean","service");
    private static final String CREATE_SUFFIX = "create";
    private static final String START_SUFFIX = "start";

    private ServiceNameFactory() {
        // forbidden instantiation
    }

    static ServiceName newCreateDestroy(final String mBeanName) {
        return newServiceName(mBeanName).append(CREATE_SUFFIX);
    }

    static ServiceName newStartStop(final String mBeanName) {
        return newServiceName(mBeanName).append(START_SUFFIX);
    }

    private static ServiceName newServiceName(final String name) {
        if(name == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("name");
        }
        return MBEAN_SERVICE_NAME_BASE.append(name);
    }

}
