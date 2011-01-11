/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.naming;

import javax.naming.Context;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A {@link NamespaceContextSelector} that can resolve the comp, module and app contexts.
 * <p>
 *
 *
 * @author Stuart Douglas
 *
 */
public class NamespaceSelectorService extends NamespaceContextSelector implements Service<NamespaceSelectorService> {

    public static final ServiceName NAME = ServiceName.of("namespaceselector");

    private final InjectedValue<Context> comp = new InjectedValue<Context>();
    private final InjectedValue<Context> module = new InjectedValue<Context>();
    private final InjectedValue<Context> app = new InjectedValue<Context>();

    private volatile boolean started = false;

    @Override
    public Context getContext(String identifier) {
        if (identifier.equals("comp")) {
            return comp.getValue();
        } else if (identifier.equals("module")) {
            return module.getValue();
        } else if (identifier.equals("app")) {
            return app.getValue();
        }
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        started = true;
    }

    @Override
    public void stop(StopContext context) {
        started = false;
    }

    public void activate() {
        if (!started) {
            throw new IllegalStateException("Service not started");
        }
        pushCurrentSelector(this);
    }

    public void deactivate() {
        popCurrentSelector();
    }

    @Override
    public NamespaceSelectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<Context> getComp() {
        return comp;
    }

    public InjectedValue<Context> getModule() {
        return module;
    }

    public InjectedValue<Context> getApp() {
        return app;
    }
}
