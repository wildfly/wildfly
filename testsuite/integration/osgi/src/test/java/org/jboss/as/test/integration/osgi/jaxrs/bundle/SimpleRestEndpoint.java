package org.jboss.as.test.integration.osgi.jaxrs.bundle;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;

@Path("/rest")
@Consumes({"application/json"})
@Produces({"application/json"})
public class SimpleRestEndpoint {

    private static final Logger log = Logger.getLogger(SimpleRestEndpoint.class);

    @Resource
    private BundleContext context;

    @GET
    @Path("/echo/{message}")
    public String echo(@PathParam("message") String message) {
        return getEchoService().echo(message);
    }

    private Echo getEchoService() {
        if (context == null) {
            log.warnf("BundleContext not injected");
            context = getBundleContextFromClass(Echo.class);
        }
        ServiceReference sref = context.getServiceReference(Echo.class.getName());
        return (Echo) context.getService(sref);
    }

    private BundleContext getBundleContextFromClass(Class<?> clazz) {
        BundleReference bref = (BundleReference) clazz.getClassLoader();
        Bundle bundle = bref.getBundle();
        if (bundle.getState() != Bundle.ACTIVE) {
            try {
                bundle.start();
            } catch (BundleException ex) {
                log.errorf(ex, "Cannot start bundle: %s", bundle);
            }
        }
        return bundle.getBundleContext();
    }
}