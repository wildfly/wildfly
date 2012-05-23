/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import javax.jms.Connection;
import javax.jms.MessageListener;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * Servlet executor consumer service
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutorConsumerService extends AbstractConsumerService<Connection> {

    public static final ServiceName NAME = ServiceName.JBOSS.append("capedwarf").append("consumer");

    private InjectedValue<ModuleLoader> loader = new InjectedValue<ModuleLoader>();

    private ServletExecutorConsumer sec;

    protected MessageListener createMessageListener() {
        sec = new ServletExecutorConsumer(loader.getValue());
        return sec;
    }

    public void removeModule(Module module) {
        if (sec != null) {
            sec.removeClassLoader(module.getClassLoader());
        }
    }

    public Connection getValue() throws IllegalStateException, IllegalArgumentException {
        return connection;
    }

    public InjectedValue<ModuleLoader> getLoader() {
        return loader;
    }
}
