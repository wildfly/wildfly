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

import org.jboss.as.pojo.BeanState;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * POJO configured phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConfiguredPojoPhase extends AbstractPojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.CONFIGURED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new CreateDestroyPojoPhase();
    }

    protected void configure(boolean nullify) throws Throwable {
        BeanUtils.configure(getBeanConfig(), getBeanInfo(), getModule(), getBean(), nullify);
    }

    @Override
    protected void startInternal(StartContext context) throws StartException {
        try {
            configure(false);
        } catch (StartException t) {
            throw t;
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.startInternal(context);
    }

    @Override
    protected void stopInternal(StopContext context) {
        super.stopInternal(context);
        try {
            configure(true);
        } catch (Throwable ignored) {
        }
    }
}
