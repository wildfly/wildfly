/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.security.sso;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Service that provides a {@link io.undertow.security.impl.SingleSignOnManager}
 * @author Paul Ferraro
 */
public class SingleSignOnManagerService implements Service<io.undertow.security.impl.SingleSignOnManager> {

    public static ServiceBuilder<io.undertow.security.impl.SingleSignOnManager> build(ServiceTarget target, ServiceName name, String serverName, String hostName) {
        ServiceName factoryName = name.append("factory");
        DistributableSingleSignOnManagerFactoryBuilder builder = new DistributableSingleSignOnManagerFactoryBuilderValue().getValue();
        if (builder != null) {
            builder.build(target, factoryName, serverName, hostName).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        } else {
            SingleSignOnManagerFactory factory = new InMemorySingleSignOnManagerFactory();
            target.addService(factoryName, new ValueService<>(new ImmediateValue<>(factory))).install();
        }
        SingleSignOnManagerService service = new SingleSignOnManagerService();
        return target.addService(name, service)
                .addDependency(factoryName, SingleSignOnManagerFactory.class, service.factory)
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class, service.host)
        ;
    }

    private final InjectedValue<SingleSignOnManagerFactory> factory = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();

    private volatile SingleSignOnManager manager;

    private SingleSignOnManagerService() {
        // Hide
    }

    @Override
    public SingleSignOnManager getValue() {
        return this.manager;
    }

    @Override
    public void start(StartContext context) {
        this.manager = this.factory.getValue().createSingleSignOnManager(this.host.getValue());
        this.manager.start();
    }

    @Override
    public void stop(StopContext context) {
        this.manager.stop();
    }
}
