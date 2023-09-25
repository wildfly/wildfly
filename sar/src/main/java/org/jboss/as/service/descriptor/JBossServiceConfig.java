/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration for a service coming from a JBoss service XML definition.
 *
 * @author John E. Bailey
 */
public class JBossServiceConfig implements Serializable {
    private static final long serialVersionUID = -1118010052087288568L;
    private String name;
    private String code;
    private String[] aliases;
    private String[] annotations;
    private JBossServiceDependencyConfig[] dependencyConfigs;
    private JBossServiceDependencyListConfig[] dependencyConfigLists;
    private JBossServiceAttributeConfig[] attributeConfigs;
    private JBossServiceConstructorConfig constructorConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public JBossServiceConstructorConfig getConstructorConfig() {
        return constructorConfig;
    }

    public void setConstructorConfig(JBossServiceConstructorConfig constructorConfig) {
        this.constructorConfig = constructorConfig;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    public JBossServiceDependencyConfig[] getDependencyConfigs() {
        return dependencyConfigs;
    }

    public void setDependencyConfigs(JBossServiceDependencyConfig[] dependencyConfigs) {
        this.dependencyConfigs = dependencyConfigs;
    }

    public JBossServiceAttributeConfig[] getAttributeConfigs() {
        return attributeConfigs;
    }

    public void setAttributeConfigs(JBossServiceAttributeConfig[] attributeConfigs) {
        this.attributeConfigs = attributeConfigs;
    }

    public JBossServiceDependencyListConfig[] getDependencyConfigLists() {
        return dependencyConfigLists;
    }

    public void setDependencyConfigLists(JBossServiceDependencyListConfig[] dependencyConfigLists) {
        this.dependencyConfigLists = dependencyConfigLists;
    }
}
