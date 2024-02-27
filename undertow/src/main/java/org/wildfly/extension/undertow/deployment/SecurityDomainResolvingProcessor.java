/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import static org.jboss.as.server.security.SecurityMetaData.ATTACHMENT_KEY;
import static org.wildfly.extension.undertow.deployment.UndertowAttachments.RESOLVED_SECURITY_DOMAIN;

import java.util.function.Predicate;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.security.SecurityMetaData;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;

/**
 * A {@code DeploymentUnitProcessor} to resolve the security domain name for the deployment.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityDomainResolvingProcessor implements DeploymentUnitProcessor {

    private static final String JAAS_CONTEXT_ROOT = "java:jboss/jaas/";
    private static final String JASPI_CONTEXT_ROOT = "java:jboss/jbsx/";
    private static final String LEGACY_JAAS_CONTEXT_ROOT = "java:/jaas/";

    private final String defaultSecurityDomain;
    private final Predicate<String> mappedSecurityDomain;

    public SecurityDomainResolvingProcessor(final String defaultSecurityDomain, final Predicate<String> mappedSecurityDomain) {
        this.defaultSecurityDomain = defaultSecurityDomain;
        this.mappedSecurityDomain = mappedSecurityDomain;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }

        final SecurityMetaData securityMetaData = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        if (securityMetaData != null && securityMetaData.getSecurityDomain() != null) {
            return; // The SecurityDomain is already defined.
        }

        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();

        String securityDomain = metaData.getSecurityDomain();
        if (securityDomain == null) {
            securityDomain = getJBossAppSecurityDomain(deploymentUnit);
        }
        securityDomain = securityDomain == null ? defaultSecurityDomain : unprefixSecurityDomain(securityDomain);

        if (securityDomain != null) {
            if (mappedSecurityDomain.test(securityDomain)) {
                ServiceName securityDomainName = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                        .getCapabilityServiceName(
                                Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN,
                                securityDomain).append(Constants.SECURITY_DOMAIN);
                if (securityMetaData != null) {
                    securityMetaData.setSecurityDomain(securityDomainName);
                }
                deploymentUnit.putAttachment(RESOLVED_SECURITY_DOMAIN, securityDomain);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(RESOLVED_SECURITY_DOMAIN);
    }

    /**
     * Try to obtain the security domain configured in jboss-app.xml at the ear level if available
     */
    private static String getJBossAppSecurityDomain(final DeploymentUnit deploymentUnit) {
        String securityDomain = null;
        DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            final EarMetaData jbossAppMetaData = parent.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (jbossAppMetaData instanceof JBossAppMetaData) {
                securityDomain = ((JBossAppMetaData) jbossAppMetaData).getSecurityDomain();
            }
        }
        return securityDomain != null ? securityDomain.trim() : null;
    }

    public static String unprefixSecurityDomain(String securityDomain) {
        String result = null;
        if (securityDomain != null)
        {
            if (securityDomain.startsWith(JAAS_CONTEXT_ROOT))
                result = securityDomain.substring(JAAS_CONTEXT_ROOT.length());
            else if (securityDomain.startsWith(JASPI_CONTEXT_ROOT))
                result = securityDomain.substring(JASPI_CONTEXT_ROOT.length());
            else if (securityDomain.startsWith(LEGACY_JAAS_CONTEXT_ROOT))
                result = securityDomain.substring(LEGACY_JAAS_CONTEXT_ROOT.length());
            else
                result = securityDomain;
        }
        return result;
    }

}
