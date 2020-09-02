package org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * Filter being called when response is being returned.
 */
@Provider
public class ResponseContextProviderFilter implements ContainerResponseFilter {

  @Override
  public void filter(ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) {

    if (!responseContext.getHeaders().containsKey("Cache-Control")) {
      responseContext.getHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
    }
    responseContext.getHeaders()
        .add("Access-Control-Allow-Headers", "origin, content-type, accept, X-XSRF-TOKEN");

    responseContext.getHeaders().add("Test-Header", "This should appear");
  }

}
