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

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.descriptor.LifecycleConfig;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * POJO lifecycle phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class LifecyclePojoPhase extends AbstractPojoPhase {
    protected abstract LifecycleConfig getUpConfig();
    protected abstract LifecycleConfig getDownConfig();
    protected abstract String defaultUp();
    protected abstract String defaultDown();

    protected void dispatchJoinpoint(LifecycleConfig config, String defaultMethod) throws Throwable {
        BeanUtils.dispatchLifecycleJoinpoint(getBeanInfo(), getBean(), config, defaultMethod);
    }

    @Override
    protected void startInternal(StartContext context) throws StartException {
        try {
            dispatchJoinpoint(getUpConfig(), defaultUp());
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.startInternal(context);
    }

    @Override
    protected void stopInternal(StopContext context) {
        super.stopInternal(context);
        try {
            dispatchJoinpoint(getDownConfig(), defaultDown());
        } catch (Throwable t) {
            PojoLogger.ROOT_LOGGER.debugf(t, "Exception at %s phase.", defaultDown());
        }
    }
}
