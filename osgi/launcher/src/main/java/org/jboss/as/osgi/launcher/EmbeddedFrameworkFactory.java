/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.osgi.launcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.logging.Logger;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * An implementation of the {@link FrameworkFactory} over an embedded server.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Dec-2012
 */
public final class EmbeddedFrameworkFactory implements FrameworkFactory {

    private static Logger log = Logger.getLogger(EmbeddedFrameworkFactory.class);

    private static final String SYSPROP_KEY_JBOSS_HOME = "jboss.home";
    private static final String SYSPROP_KEY_MODULE_PATH = "module.path";
    private static final String SYSPROP_KEY_BUNDLE_PATH = "bundle.path";
    private static final String SYSPROP_KEY_JBOSS_SERVER_CONFIG = "jboss.server.config.file.name";

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Framework newFramework(final Map configuration) {

        // Make a defensive copy of the configuration
        Map<String, String> props = new HashMap<String, String>();
        if (configuration != null) {
            props.putAll(configuration);
        }

        // Put all properties on the System
        // [TODO] remove this hack and configure the osgi subsystem properly
        for (Map.Entry<String, String> entry : props.entrySet()) {
            SecurityActions.setSystemProperty(entry.getKey(), entry.getValue());
        }

        log.debugf("Config: " + props);

        Set<String> syspackages = new HashSet<String>();
        syspackages.add("org.osgi.framework");
        syspackages.add("org.osgi.resource");
        syspackages.add("org.osgi.util.tracker");

        addConfiguredPackages(syspackages, props, Constants.FRAMEWORK_SYSTEMPACKAGES);
        addConfiguredPackages(syspackages, props, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        addConfiguredPackages(syspackages, props, Constants.FRAMEWORK_BOOTDELEGATION);

        String jbossHome = getProperty(props, SYSPROP_KEY_JBOSS_HOME, null);
        String modulePath = getProperty(props, SYSPROP_KEY_MODULE_PATH, null);
        String bundlePath = getProperty(props, SYSPROP_KEY_BUNDLE_PATH, null);
        String serverConfig = getProperty(props, SYSPROP_KEY_JBOSS_SERVER_CONFIG, "standalone-osgi.xml");
        String[] sysarray = syspackages.toArray(new String[syspackages.size()]);
        String[] cmdargs = new String[] { CommandLineConstants.SERVER_CONFIG, serverConfig };

        return new FrameworkProxy(EmbeddedServerFactory.create(jbossHome, modulePath, bundlePath, sysarray, cmdargs));
    }

    private void addConfiguredPackages(Set<String> syspackages, Map<String, String> props, String key) {
        String value = props.get(key);
        if (value != null) {
            // [TODO] Replace with ElementParser. The code below will fail for attributes/directives that contain ','
            for (String token : value.split(",\\s")) {
                int index = token.indexOf(';');
                if (index > 0) {
                    token = token.substring(0, index);
                }
                syspackages.add(token);
            }
        }
    }

    private String getProperty(Map<String, String> props, String key, String defaultValue) {
        String value = props.get(key);
        if (value == null) {
            value = SecurityActions.getSystemProperty(key);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
