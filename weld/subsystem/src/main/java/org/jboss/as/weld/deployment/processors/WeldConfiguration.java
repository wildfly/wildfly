/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * The final configuration for Weld where the global configuration defined in the model is combined in per-deployment configuration specified in
 * <code>jboss-all.xml</code>
 *
 * @author Jozef Hartinger
 *
 */
class WeldConfiguration {

    public static final AttachmentKey<WeldConfiguration> ATTACHMENT_KEY = AttachmentKey.create(WeldConfiguration.class);

    private final boolean requireBeanDescriptor;
    private final boolean nonPortableMode;
    private final boolean developmentMode;
    private final boolean legacyEmptyBeansXmlTreatment;

    public WeldConfiguration(boolean requireBeanDescriptor, boolean nonPortableMode, boolean developmentMode, boolean legacyEmptyBeansXmlTreatment) {
        this.requireBeanDescriptor = requireBeanDescriptor;
        this.nonPortableMode = nonPortableMode;
        this.developmentMode = developmentMode;
        this.legacyEmptyBeansXmlTreatment = legacyEmptyBeansXmlTreatment;
    }

    public boolean isNonPortableMode() {
        return nonPortableMode;
    }

    public boolean isRequireBeanDescriptor() {
        return requireBeanDescriptor;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    public boolean isLegacyEmptyBeansXmlTreatment() {
        return legacyEmptyBeansXmlTreatment;
    }

    @Override
    public String toString() {
        return "WeldConfiguration [requireBeanDescriptor=" + requireBeanDescriptor + ", nonPortableMode=" + nonPortableMode + ", developmentMode="
                + developmentMode + ", legacyEmptyBeansXmlTreatment=" + legacyEmptyBeansXmlTreatment + "]";
    }

}
