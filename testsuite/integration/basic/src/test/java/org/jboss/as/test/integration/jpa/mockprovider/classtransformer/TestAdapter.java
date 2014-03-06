package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import java.util.Map;

import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;

/**
 * persistence provider adaptor test class
 *
 * @author Scott Marlow
 */
public class TestAdapter implements PersistenceProviderAdaptor {

    private static volatile boolean initialized = false;

    @Override
    public void injectJtaManager(JtaManager jtaManager) {

    }

    @Override
    public void injectPlatform(Platform platform) {

    }

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {

    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {

    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        initialized = true;
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {

    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return null;
    }

    @Override
    public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
        return false;
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {

    }

    public static boolean wasInitialized() {
        return initialized;
    }

    public static void clearInitialized() {
        initialized = false;
    }
}
