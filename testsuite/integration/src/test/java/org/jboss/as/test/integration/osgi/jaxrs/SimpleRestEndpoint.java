package org.jboss.as.test.integration.osgi.jaxrs;

import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.Collection;

@Path("/cm")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public class SimpleRestEndpoint {

    private static final Logger log = Logger.getLogger(SimpleRestEndpoint.class);

    @Resource
    private BundleContext context;

    @GET
    @Path("/pids")
    public Collection<String> listConfigurations() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            for (Configuration config : getConfigurationAdmin().listConfigurations(null)) {
                list.add(config.getPid());
            }
        } catch (Exception ex) {
            log.errorf(ex, "Cannot list pids");
        }
        return list;
    }

    // [TODO] initialize this in a defined lifecycle step
    private ConfigurationAdmin service;
    private ConfigurationAdmin getConfigurationAdmin() {
        if (service == null) {

            if (context == null) {
                log.warnf("BundleContext not injected");
                context = getBundleContextFromClass();
            }

            ServiceTracker tracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null) {

                @Override
                public Object addingService(ServiceReference sref) {
                    service = (ConfigurationAdmin) super.addingService(sref);
                    log.infof("Adding service: %s", service);
                    return service;
                }

                @Override
                public void removedService(ServiceReference sref, Object instance) {
                    super.removedService(sref, service);
                    log.infof("Removing service: %s", service);
                    service = null;
                }
            };
            tracker.open();
        }
        return service;
    }

    private BundleContext getBundleContextFromClass() {
        BundleReference bref = (BundleReference) ConfigurationAdmin.class.getClassLoader();
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

    private static final long serialVersionUID = 1L;
}