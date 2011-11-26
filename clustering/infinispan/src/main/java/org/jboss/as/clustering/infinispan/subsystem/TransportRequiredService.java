package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service which maintains a AtomicBoolean value to indicate whether clustering
 * caches are defined.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportRequiredService implements Service<AtomicBoolean> {

    // private final static String SERVICE_NAME_ELEMENT = "transportRequired" ;
    private  AtomicBoolean transportRequired = null;

    public static ServiceName getServiceName(String container) {
        return EmbeddedCacheManagerService.getServiceName(container).append("transportRequired");
    }

    public static ServiceName getServiceName(ServiceName container) {
        return container.append("transportRequired");
    }

    public TransportRequiredService(AtomicBoolean value) {
        // nothing to do
        this.transportRequired = value ;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    public AtomicBoolean getValue() {
        return this.transportRequired;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    public void start(StartContext context) throws StartException {
       // transportRequired.set(false);
     }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    public void stop(StopContext context) {
         //
    }
}



