package org.jboss.as.test.clustering.unmanaged.singleton.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class MyService implements Service<String> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice");
    
    private final InjectedValue<ServerEnvironment> env = new InjectedValue<ServerEnvironment>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public Injector<ServerEnvironment> getEnvInjector() {
        return this.env;
    }

    @Override
    public String getValue() {
        if (!this.started.get()) {
            throw new IllegalStateException();
        }
        return env.getValue().getNodeName();
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.started.set(true);
    }

    @Override
    public void stop(StopContext context) {
        this.started.set(false);
    }
}
