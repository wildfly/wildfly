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
