/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.version;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Common location to manage the AS based product name and version.
 *
 */
public class ProductConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String version;
    private final String consoleSlot;

    public ProductConfig(ModuleLoader loader, String home, Map<?, ?> providedProperties) {
        String productName = null;
        String productVersion = null;
        String consoleSlot = null;

        FileReader reader = null;
        try {
            reader = new FileReader(getProductConf(home));
            Properties props = new Properties();
            props.load(reader);

            String slot = (String) props.get("slot");
            if (slot != null) {
                Module module = loader.loadModule(ModuleIdentifier.create("org.jboss.as.product", slot));

                InputStream stream = module.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
                Manifest manifest = null;
                if (stream != null) {
                    manifest = new Manifest(stream);
                }

                if (manifest != null) {
                    productName = manifest.getMainAttributes().getValue("JBoss-Product-Release-Name");
                    productVersion = manifest.getMainAttributes().getValue("JBoss-Product-Release-Version");
                    consoleSlot = manifest.getMainAttributes().getValue("JBoss-Product-Console-Slot");
                }
            }

            setSystemProperties(props, providedProperties);
        } catch (Exception e) {
            // Don't care
        } finally {
            safeClose(reader);
        }

        name = productName;
        version = productVersion;
        this.consoleSlot = consoleSlot;
    }

    private static String getProductConf(String home) {
        final String defaultVal = home + File.separator + "bin" + File.separator + "product.conf";
        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            public String run() {
                String env = System.getenv("JBOSS_PRODUCT_CONF");
                if (env == null) {
                    env = defaultVal;
                }
                return env;
            }
        };

        return System.getSecurityManager() == null ? action.run() : AccessController.doPrivileged(action);
    }

    /** Solely for use in unit testing */
    public ProductConfig(final String productName, final String productVersion, final String consoleSlot) {
        this.name = productName;
        this.version = productVersion;
        this.consoleSlot = consoleSlot;
    }

    public String getProductName() {
        return name;
    }

    public String getProductVersion() {
        return version;
    }

    public String getConsoleSlot() {
        return consoleSlot;
    }

    public String getPrettyVersionString() {
        if (name != null)
           return String.format("JBoss %s %s (AS %s)", name, version, Version.AS_VERSION);

        return String.format("WildFly %s \"%s\"", Version.AS_VERSION, Version.AS_RELEASE_CODENAME);
    }

    public static String getPrettyVersionString(final String name, String version1, String version2) {
        if(name != null) {
            return String.format("JBoss %s %s (AS %s)", name, version1, version2);
        }
        return String.format("WildFly %s \"%s\"", version1, version2);
    }

    private void setSystemProperties(final Properties propConfProps, final Map providedProperties) {
        if (propConfProps.size() == 0) {
            return;
        }

        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                for (Map.Entry<Object, Object> entry : propConfProps.entrySet()) {
                    String key = (String)entry.getKey();
                    if (!key.equals("slot") && System.getProperty(key) == null) {
                        //Only set the property if it was not defined by other means
                        //System properties defined in standalone.xml, domain.xml or host.xml will overwrite
                        //this as specified in https://issues.jboss.org/browse/AS7-6380

                        System.setProperty(key, (String)entry.getValue());

                        //Add it to the provided properties used on reload by the server environment
                        providedProperties.put(key, entry.getValue());
                    }
                }
                return null;
            }
        };
        if (System.getSecurityManager() == null) {
            action.run();
        } else {
            AccessController.doPrivileged(action);
        }
    }

    private static void safeClose(Closeable c) {
        if (c != null) try {
            c.close();
        } catch (Throwable ignored) {}
    }

}
