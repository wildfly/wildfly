/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration for a service attribute.
 *
 * @author John E. Bailey
 */
public class JBossServiceAttributeConfig implements Serializable {
    private static final long serialVersionUID = 7859894445434159600L;

    private String name;
    private boolean replace;
    private boolean trim;
    private String value;

    private ValueFactory valueFactory;
    private Inject inject;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public boolean isTrim() {
        return trim;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    public Inject getInject() {
        return inject;
    }

    public void setInject(Inject inject) {
        this.inject = inject;
    }

    public static class ValueFactory implements Serializable {
        private static final long serialVersionUID = 2524264651820839136L;
        private String beanName;
        private String methodName;
        private ValueFactoryParameter[] parameters;

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public ValueFactoryParameter[] getParameters() {
            return parameters;
        }

        public void setParameters(ValueFactoryParameter[] parameters) {
            this.parameters = parameters;
        }
    }

    public static class ValueFactoryParameter implements Serializable {
        private static final long serialVersionUID = -1980437946334603841L;
        private String type;
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Inject implements Serializable {
        private static final long serialVersionUID = 7644229980407045584L;

        private String beanName;
        private String propertyName;

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
