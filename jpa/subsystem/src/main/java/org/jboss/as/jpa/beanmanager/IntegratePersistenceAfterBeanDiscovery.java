package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

/**
 * IntegratePersistenceAfterBeanDiscovery
 *
 * @author Scott Marlow
 */
public class IntegratePersistenceAfterBeanDiscovery implements Extension {

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {

    }

}
