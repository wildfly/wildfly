package org.jboss.as.jpa.beanmanager;

import java.util.List;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Stub for Jakarta EE 11/Persistence 3.2 SchemaManager class which is not present in Jakarta EE 10 therefore this class
 * will not create a CDI bean for accessing the SchemaManager.
 * We do expect that for EE 11, we will have a different implementation of this class that does create a bean for
 * accessing the SchemaManager.
 *
 * @author Scott Marlow
 */
public class Persistence32 {

    static void schemaManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

    }
}

