/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.jsf.subsystem.JSFResourceDefinition;
import org.jboss.as.controller.ModuleIdentifierUtil;

/**
 * This class finds all the installed Jakarta Server Faces implementations and provides their ModuleId's.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFModuleIdFactory {
    private static final String API_MODULE = "jakarta.faces.api";
    private static final String IMPL_MODULE = "jakarta.faces.impl";
    private static final String INJECTION_MODULE = "org.jboss.as.jsf-injection";

    private static final JSFModuleIdFactory instance = new JSFModuleIdFactory();

    // The default JSF impl slot.  This can be overridden by the management layer.
    private String defaultSlot = JSFResourceDefinition.DEFAULT_SLOT;

    private Map<String, String> apiIds = new HashMap<>();
    private Map<String, String> implIds = new HashMap<>();
    private Map<String, String> injectionIds = new HashMap<>();

    private Set<String> allVersions = new HashSet<>();
    private List<String> activeVersions = new ArrayList<>();

    public static JSFModuleIdFactory getInstance() {
        return instance;
    }

    private JSFModuleIdFactory() {
        String modulePath = System.getProperty("module.path", System.getenv("JAVA_MODULEPATH"));
        if (!isBogusPath(modulePath)) {
            loadIdsFromModulePath(modulePath);
        }

        if (!activeVersions.contains("main")) {
            loadIdsManually();
        }

        JSFLogger.ROOT_LOGGER.activatedJSFImplementations(activeVersions);
    }

    void setDefaultSlot(String defaultSlot) {
        this.defaultSlot = defaultSlot;
    }

    String getDefaultSlot() {
        return this.defaultSlot;
    }

    private boolean isBogusPath(String path) {
        if (path == null) return true;

        // must have at least one existing directory in the path
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir).exists()) return false;
        }

        return true; // no directory in the path exists
    }

    // just provide the default implementations
    private void loadIdsManually() {
        implIds.put("main", ModuleIdentifierUtil.canonicalModuleIdentifier(IMPL_MODULE, "main"));
        apiIds.put("main", ModuleIdentifierUtil.canonicalModuleIdentifier(API_MODULE, "main"));
        injectionIds.put("main", ModuleIdentifierUtil.canonicalModuleIdentifier(INJECTION_MODULE, "main"));

        allVersions.add("main");

        activeVersions.add("main");
    }

    private void loadIdsFromModulePath(String modulePath) {
        for (String moduleRootDir : modulePath.split(File.pathSeparator)) {
            loadIds(moduleRootDir, apiIds, API_MODULE);
            loadIds(moduleRootDir, implIds, IMPL_MODULE);
            loadIds(moduleRootDir, injectionIds, INJECTION_MODULE);
        }
        checkVersionIntegrity();
    }

    private void loadIds(String moduleRootDir, Map<String, String> idMap, String moduleName) {
        StringBuilder baseDirBuilder = new StringBuilder(moduleRootDir);
        baseDirBuilder.append(File.separator);
        baseDirBuilder.append(moduleName.replace(".", File.separator));

        File moduleBaseDir = new File(baseDirBuilder.toString());
        File[] slots = moduleBaseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        if (slots == null) return;

        for (File slot : slots) {
            if (!new File(slot, "module.xml").exists()) continue; // make sure directory represents a real module
            String slotName = slot.getName();
            allVersions.add(slotName);
            idMap.put(slotName, ModuleIdentifierUtil.canonicalModuleIdentifier(moduleName, slotName));
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
    String computeSlot(String jsfVersion) {
        if (jsfVersion == null) return defaultSlot;
        if (JsfVersionMarker.JSF_4_0.equals(jsfVersion)) return defaultSlot;
        return jsfVersion;
    }

    String getApiModId(String jsfVersion) {
        return this.apiIds.get(computeSlot(jsfVersion));
    }

    String getImplModId(String jsfVersion) {
        return this.implIds.get(computeSlot(jsfVersion));
    }

    String getInjectionModId(String jsfVersion) {
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
    public List<String> getActiveJSFVersions() {
        return Collections.unmodifiableList(activeVersions);
    }

}
