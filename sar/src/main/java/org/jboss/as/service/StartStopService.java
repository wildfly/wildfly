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

package org.jboss.as.service;

import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for legacy JBoss services that controls the service start and stop lifecycle.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class StartStopService extends AbstractService {

    private static final String START_METHOD_NAME = "start";
    private static final String STOP_METHOD_NAME = "stop";

    StartStopService(final Object mBeanInstance, final ClassReflectionIndex<?> mBeanClassIndex) {
        super(mBeanInstance, mBeanClassIndex);
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        log.debugf("Starting Service: %s", context.getController().getName());
        try {
            invokeLifecycleMethod(START_METHOD_NAME);
        } catch (final Exception e) {
            throw new StartException("Failed to execute legacy service start() method", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        log.debugf("Stopping Service: %s", context.getController().getName());
        try {
            invokeLifecycleMethod(STOP_METHOD_NAME);
        } catch (final Exception e) {
            log.error("Failed to execute legacy service stop() method", e);
        }
    }

}
