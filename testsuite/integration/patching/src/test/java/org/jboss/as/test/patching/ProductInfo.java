package org.jboss.as.test.patching;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * @author Emanuel Muckenhuber
 */
class ProductInfo {

    public static final String PRODUCT_VERSION;
    public static final String PRODUCT_NAME;
    public static final boolean isProduct;

    static {

        // Load the current product conf

        final File distributionRoot = new File(PatchingTestUtil.AS_DISTRIBUTION);
        final LocalModuleLoader loader = new LocalModuleLoader(new File[] { PatchingTestUtil.BASE_MODULE_DIRECTORY });
        try {
            final Module module = loader.loadModule(ModuleIdentifier.create("org.jboss.as.version"));

            final Class<?> clazz = module.getClassLoader().loadClass("org.jboss.as.version.ProductConfig");
            final Method resolveName = clazz.getMethod("resolveName");
            final Method resolveVersion = clazz.getMethod("resolveVersion");
            final Method productName = clazz.getMethod("getProductName");
            final Constructor<?> constructor = clazz.getConstructor(ModuleLoader.class, String.class, Map.class);

            final Object productConfig = constructor.newInstance(loader, distributionRoot.getAbsolutePath(), Collections.emptyMap());

            PRODUCT_NAME = (String) resolveName.invoke(productConfig);
            PRODUCT_VERSION = (String) resolveVersion.invoke(productConfig);

            isProduct = productName.invoke(productConfig) != null; // See if the product name was configured

        } catch (Exception e) {
            throw new RuntimeException(PatchingTestUtil.BASE_MODULE_DIRECTORY.getAbsolutePath(), e);
        }
    }

    static String getVersionModule() {
        return isProduct ? "org.jboss.as.product" : "org.jboss.as.version";
    }

    static String getVersionModuleSlot() {
        return isProduct ? "eap" : "main";
    }

    static String createVersionString(final String newVersion) {
        final StringBuilder builder = new StringBuilder();
        if (isProduct) {
            builder.append("JBoss-Product-Release-Name: ").append(PRODUCT_NAME).append("\n");
            builder.append("JBoss-Product-Release-Version: ").append(newVersion).append("\n");
            builder.append("JBoss-Product-Console-Slot: eap").append("\n");
        } else {
            builder.append("JBossAS-Release-Version: ").append(newVersion).append("\n");
        }
        return builder.toString();
    }

}
