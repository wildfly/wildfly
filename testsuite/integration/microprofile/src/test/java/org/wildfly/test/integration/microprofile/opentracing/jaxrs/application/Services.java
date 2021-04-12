package org.wildfly.test.integration.microprofile.opentracing.jaxrs.application;

import org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.services.EndpointService;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath(Services.APP_PATH)
public class Services extends Application {

  public static final String APP_PATH = "/test";

  @Override
  public Set<Class<?>> getClasses() {
    return Stream.of(EndpointService.class).collect(Collectors.toSet());
  }

}
