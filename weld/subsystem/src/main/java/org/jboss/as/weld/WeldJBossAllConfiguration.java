/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Immutable representation of Weld configuration in <code>jboss-all.xml<code>.
 * This configuration is processed by {@link org.jboss.as.weld.deployment.processors.WeldConfigurationProcessor}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldJBossAllConfiguration {

    public static final AttachmentKey<WeldJBossAllConfiguration> ATTACHMENT_KEY = AttachmentKey.create(WeldJBossAllConfiguration.class);

    private final Boolean requireBeanDescriptor;
    private final Boolean nonPortableMode;
    private final Boolean developmentMode;
    private final Boolean legacyEmptyBeansXmlTreatment;

    WeldJBossAllConfiguration(Boolean requireBeanDescriptor, Boolean nonPortableMode, Boolean developmentMode, Boolean legacyEmptyBeansXmlTreatment) {
        this.requireBeanDescriptor = requireBeanDescriptor;
        this.nonPortableMode = nonPortableMode;
        this.developmentMode = developmentMode;
        this.legacyEmptyBeansXmlTreatment = legacyEmptyBeansXmlTreatment;
    }

    public Boolean getNonPortableMode() {
        return nonPortableMode;
    }

    public Boolean getRequireBeanDescriptor() {
        return requireBeanDescriptor;
    }

    public Boolean getDevelopmentMode() {
        return developmentMode;
    }

    public Boolean getLegacyEmptyBeansXmlTreatment() {
        return legacyEmptyBeansXmlTreatment;
    }
}
