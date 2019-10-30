package org.jboss.as.webservices.webserviceref;

import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.ws.common.utils.DelegateClassLoader;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefFactory;
import org.jboss.wsf.spi.serviceref.ServiceRefFactoryFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A managed reference factory for web service refs
 *
 *
 * @author Stuart Douglas
 */
public class WebServiceManagedReferenceFactory implements ManagedReferenceFactory {

    private final UnifiedServiceRefMetaData serviceRef;
    private final ClassLoader classLoader;

    public WebServiceManagedReferenceFactory(final UnifiedServiceRefMetaData serviceRef, final ClassLoader classLoader) {
        this.serviceRef = serviceRef;
        this.classLoader = new DelegateClassLoader(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader(), classLoader);
    }

    @Override
    public ManagedReference getReference() {
        final ClassLoader oldCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            final ServiceRefFactory serviceRefFactory = getServiceRefFactory();
            return new ImmediateManagedReference(serviceRefFactory.newServiceRef(serviceRef));
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCL);
        }
    }

    private ServiceRefFactory getServiceRefFactory() {
        final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
        return spiProvider.getSPI(ServiceRefFactoryFactory.class).newServiceRefFactory();
    }
}
