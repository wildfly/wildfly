package org.wildfly.test.integration.microprofile.restclient;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("api")
public class ServerServiceApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(ServerResource.class);
        classes.add(ClientResource.class);
        return classes;
    }
}
