/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.jsf.deployment;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.jsf.JSFLogger;
import org.jboss.modules.ModuleIdentifier;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFModuleIdFactory {
    public static final String DEFAULT_SLOT = "main";

    private static final String API_MODULE = "javax.faces.api";
    private static final String IMPL_MODULE = "com.sun.jsf-impl";
    private static final String INJECTION_MODULE = "org.jboss.as.jsf-injection";

    private static final JSFModuleIdFactory instance = new JSFModuleIdFactory();

    private Map<String, ModuleIdentifier> apiIds = new HashMap<String, ModuleIdentifier>();
    private Map<String, ModuleIdentifier> implIds = new HashMap<String, ModuleIdentifier>();
    private Map<String, ModuleIdentifier> injectionIds = new HashMap<String, ModuleIdentifier>();

    private Set<String> allVersions = new HashSet<String>();
    private List<String> activeVersions = new ArrayList<String>();

    public static JSFModuleIdFactory getInstance() {
        return instance;
    }

    private JSFModuleIdFactory() {
        String modulePath = System.getProperty("module.path", System.getenv("JAVA_MODULEPATH"));

        if (modulePath == null) {
            loadIdsManually();
        } else {
            loadIdsFromModulePath(modulePath);
        }
    }

    // just provide the default implementations
    private void loadIdsManually() {
        implIds.put("main", ModuleIdentifier.create(IMPL_MODULE));
        apiIds.put("main", ModuleIdentifier.create(API_MODULE));
        injectionIds.put("main", ModuleIdentifier.create(INJECTION_MODULE));

        implIds.put("1.2", ModuleIdentifier.create(IMPL_MODULE, "1.2"));
        apiIds.put("1.2", ModuleIdentifier.create(API_MODULE, "1.2"));
        injectionIds.put("1.2", ModuleIdentifier.create(INJECTION_MODULE, "1.2"));

        allVersions.add("main");
        allVersions.add("1.2");

        activeVersions.add("main");
        activeVersions.add("1.2");
    }

    private void loadIdsFromModulePath(String modulePath) {
        for (String moduleRootDir : modulePath.split(File.pathSeparator)) {
            loadIds(moduleRootDir, apiIds, API_MODULE);
            loadIds(moduleRootDir, implIds, IMPL_MODULE);
            loadIds(moduleRootDir, injectionIds, INJECTION_MODULE);
        }
        checkVersionIntegrity();
    }

    private void loadIds(String moduleRootDir, Map<String, ModuleIdentifier> idMap, String moduleName) {
        StringBuilder baseDirBuilder = new StringBuilder(moduleRootDir);
        baseDirBuilder.append(File.separator);
        baseDirBuilder.append(moduleName.replace(".", File.separator));

        File moduleBaseDir = new File(baseDirBuilder.toString());
        File[] slots = moduleBaseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File slot : slots) {
            if (!new File(slot, "module.xml").exists()) continue; // make sure directory represents a real module
            String slotName = slot.getName();
            allVersions.add(slotName);
            idMap.put(slotName, ModuleIdentifier.create(moduleName, slotName));
        }
    }

    // make sure that each version has api, impl, and injection
    private void checkVersionIntegrity() {
        activeVersions.addAll(allVersions);

        for (String version : allVersions) {
            if (!apiIds.containsKey(version)) {
                JSFLogger.ROOT_LOGGER.missingJSFModule(version, API_MODULE);
                activeVersions.remove(version);
            }

            if (!implIds.containsKey(version)) {
                JSFLogger.ROOT_LOGGER.missingJSFModule(version, IMPL_MODULE);
                activeVersions.remove(version);
            }

            if (!injectionIds.containsKey(version)) {
                JSFLogger.ROOT_LOGGER.missingJSFModule(version, INJECTION_MODULE);
                activeVersions.remove(version);
            }
        }
    }

    /**
     * If needed, convert old JSFVersionMarker values to slot values.
     *
     * @param jsfVersion The version value from JSFVersionMarker, or null for default slot.
     * @return The equivalent slot value.
     */
    static String computeSlot(String jsfVersion) {
        if (jsfVersion == null) return DEFAULT_SLOT;
        if (JsfVersionMarker.JSF_2_0.equals(jsfVersion)) return DEFAULT_SLOT;
        if (JsfVersionMarker.JSF_1_2.equals(jsfVersion)) return "1.2";
        return jsfVersion;
    }

    ModuleIdentifier getApiModId(String jsfVersion) {
        return this.apiIds.get(computeSlot(jsfVersion));
    }

    ModuleIdentifier getImplModId(String jsfVersion) {
        return this.implIds.get(computeSlot(jsfVersion));
    }

    ModuleIdentifier getInjectionModId(String jsfVersion) {
        return this.injectionIds.get(computeSlot(jsfVersion));
    }

    boolean isValidJSFSlot(String slot) {
        String computedSlot = computeSlot(slot);
        return apiIds.containsKey(computedSlot) && implIds.containsKey(computedSlot) && injectionIds.containsKey(computedSlot);
    }

    /**
     * Return the slot id's of all active JSF versions.
     *
     * @return The slot id's of all active JSF versions.
     */
    List<String> getActiveJSFVersions() {
        return Collections.unmodifiableList(activeVersions);
    }

}
