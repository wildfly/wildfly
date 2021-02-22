package org.wildfly.test.integration.microprofile.opentracing.application;

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import io.opentracing.Tracer;

/**
 * @author Sultan Zhantemirov (c) 2019 Red Hat, Inc.
 */
@ApplicationPath("tracer-config")
public class TracerConfigurationApplication extends Application {

    @Path("/get")
    public static class TestResource {

        @Inject
        private Tracer tracer;

        @GET
        @Produces("text/plain")
        public String get() {
            if (checkClass(this.tracer, "JaegerTracer")) {
                return tracer.toString() + getSenderConfiguration();
            }
            return tracer.toString();
        }

        private String getSenderConfiguration() {
            try {
                Field reporterField = this.tracer.getClass().getDeclaredField("reporter");
                reporterField.setAccessible(true);
                Object reporter = reporterField.get(this.tracer);
                if (checkClass(reporter, "RemoteReporter")) {
                    Field senderField = reporter.getClass().getDeclaredField("sender");
                    senderField.setAccessible(true);
                    Object sender = senderField.get(reporter);
                    if (checkClass(sender, "UdpSender")) {
                        Field portField = sender.getClass().getDeclaredField("port");
                        portField.setAccessible(true);
                        Object port = portField.get(sender);

                        Field hostField = sender.getClass().getDeclaredField("host");
                        hostField.setAccessible(true);
                        Object host = hostField.get(sender);

                        if (host != null && port != null) {
                            return ", sender-binding=" + host + ":" + port;
                        }
                    }
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Error getting network configuration details", ex);
            }
            return "";
        }

        private boolean checkClass(Object obj, String simpleName) {
            return obj != null && simpleName.equals(obj.getClass().getSimpleName());
        }
    }
}
