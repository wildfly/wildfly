/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    private static final String REGISTRATION_SUFFIX = "registration";
    private ServiceNameFactory() {
        // forbidden instantiation
    }

    static ServiceName newCreateDestroy(final String mBeanName) {
        return newServiceName(mBeanName).append(CREATE_SUFFIX);
    }

    static ServiceName newStartStop(final String mBeanName) {
        return newServiceName(mBeanName).append(START_SUFFIX);
    }

    static ServiceName newRegisterUnregister(final String mBeanName) {
        return newServiceName(mBeanName).append(REGISTRATION_SUFFIX);
    }

    private static ServiceName newServiceName(final String name) {
        if(name == null) {
            throw SarLogger.ROOT_LOGGER.nullVar("name");
        }
        return MBEAN_SERVICE_NAME_BASE.append(name);
    }

}
