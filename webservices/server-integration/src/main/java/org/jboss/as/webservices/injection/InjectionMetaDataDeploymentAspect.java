/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.injection;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.metadata.WebServiceDeclaration;
import org.jboss.as.webservices.metadata.WebServiceDeployment;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
//import org.jboss.ejb3.ejbref.resolver.spi.EjbReferenceResolver; TODO: replace
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.ws.common.injection.resolvers.ResourceReferenceResolver;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.metadata.injection.InjectionMetaData;
import org.jboss.wsf.spi.metadata.injection.InjectionsMetaData;
import org.jboss.wsf.spi.metadata.injection.ReferenceResolver;

/**
 * Deployment aspect that builds injection meta data.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
public final class InjectionMetaDataDeploymentAspect extends AbstractDeploymentAspect {

    /** Resolver handling @Resource injections. */
    private static final ReferenceResolver RESOURCE_RESOLVER = new ResourceReferenceResolver();

    /** EJB 3 reference resolver. */
    // private EjbReferenceResolver ejbReferenceResolver; // TODO: replace

    /**
     * Constructor.
     */
    public InjectionMetaDataDeploymentAspect() {
        super();
    }

    /**
     * Sets ejb reference resolver. This method is invoked by MC.
     *
     * @param resolver ejb reference resolver
     */
    /*
     * TODO: uncomment public void setEjbReferenceResolver(final EjbReferenceResolver resolver) { this.ejbReferenceResolver =
     * resolver; }
     */

    /**
     * Gets ejb reference resolver.
     *
     * @return ejb reference resolver
     */
    /*
     * TODO: uncomment public EjbReferenceResolver getEjbReferenceResolver() { if (this.ejbReferenceResolver == null) { throw
     * new IllegalStateException(BundleUtils.getMessage(bundle, "NO_EJBREFERENCERESOLVER_SET_BY_MC")); }
     *
     * return this.ejbReferenceResolver; }
     */

    /**
     * Builds injection meta data for all endpoints in deployment.
     *
     * @param dep webservice deployment
     */
    @Override
    public void start(final Deployment dep) {
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        final JBossWebMetaData jbossWebMD = WSHelper.getRequiredAttachment(dep, JBossWebMetaData.class);
        final Map<Class<? extends Annotation>, ReferenceResolver> resolvers = this.getResolvers(unit);

        if (WSHelper.isJaxwsJseDeployment(dep)) {
            this.log.debug("Building injection meta data for JAXWS JSE webservice deployment: " + dep.getSimpleName());
            final EnvironmentEntriesMetaData envEntriesMD = jbossWebMD.getEnvironmentEntries();

            // iterate through all POJO endpoints
            for (Endpoint endpoint : dep.getService().getEndpoints()) {
                // build POJO injections meta data
                final InjectionsMetaData injectionsMD = this.buildInjectionsMetaData(envEntriesMD, resolvers);

                // associate injections meta data with POJO endpoint
                endpoint.addAttachment(InjectionsMetaData.class, injectionsMD);
            }
        } else if (WSHelper.isJaxwsEjbDeployment(dep)) {
            this.log.debug("Building injection meta data for JAXWS EJB3 webservice deployment: " + dep.getSimpleName());
            final WebServiceDeployment webServiceDeployment = ASHelper.getRequiredAttachment(unit,
                    WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY);
            final Service service = dep.getService();

            // iterate through all EJB3 endpoints
            for (final WebServiceDeclaration container : webServiceDeployment.getServiceEndpoints()) {
                /*
                 * TODO: uncomment final String ejbName = container.getComponentName(); final Endpoint endpoint =
                 * service.getEndpointByName(ejbName); if (endpoint != null && ASHelper.isWebServiceBean(container)) { // build
                 * EJB 3 injections meta data final EnvironmentEntriesMetaData ejbEnvEntries =
                 * this.getEnvironmentEntries(ejbName, unit); final InjectionsMetaData injectionsMD =
                 * this.buildInjectionsMetaData(ejbEnvEntries, resolvers);
                 *
                 * // associate injections meta data with EJB 3 endpoint endpoint.addAttachment(InjectionsMetaData.class,
                 * injectionsMD); }
                 */
            }
        }
    }

    /**
     * Returns environment entries meta data associated with specified EJB 3 bean.
     *
     * @param ejbName EJB 3 bean to lookup environment entries for
     * @param unit deployment unit
     * @return environment entries meta data
     */
    /*
     * TODO: uncomment private EnvironmentEntriesMetaData getEnvironmentEntries(final String ejbName, final DeploymentUnit unit)
     * { final JBossMetaData jbossMD = ASHelper.getRequiredAttachment(unit, JBossMetaData.class); final
     * JBossEnterpriseBeansMetaData enterpriseBeansMDs = jbossMD.getEnterpriseBeans();
     *
     * return enterpriseBeansMDs.get(ejbName).getEnvironmentEntries(); }
     */

    /**
     * Returns reference resolvers container.
     *
     * @param unit deployment unit
     * @return reference resolvers
     */
    private Map<Class<? extends Annotation>, ReferenceResolver> getResolvers(final DeploymentUnit unit) {
        final Map<Class<? extends Annotation>, ReferenceResolver> resolvers = new HashMap<Class<? extends Annotation>, ReferenceResolver>();

        resolvers.put(Resource.class, InjectionMetaDataDeploymentAspect.RESOURCE_RESOLVER);
        // resolvers.put(EJB.class, new EJBBeanReferenceResolver(unit, this.getEjbReferenceResolver())); TODO: uncomment

        return resolvers;
    }

    /**
     * Builds JBossWS specific injections meta data.
     *
     * @param envEntriesMD environment entries meta data
     * @param resolvers known annotation resolvers
     * @param jndiContext JNDI context to be propagated
     */
    private InjectionsMetaData buildInjectionsMetaData(final EnvironmentEntriesMetaData envEntriesMD,
            final Map<Class<? extends Annotation>, ReferenceResolver> resolvers) {
        final List<InjectionMetaData> injectionMD = new LinkedList<InjectionMetaData>();
        injectionMD.addAll(this.buildInjectionMetaData(envEntriesMD));

        return new InjectionsMetaData(injectionMD, resolvers);
    }

    /**
     * Builds JBossWS specific injection meta data.
     *
     * @param envEntriesMD environment entries meta data
     * @return injection meta data
     */
    private List<InjectionMetaData> buildInjectionMetaData(final EnvironmentEntriesMetaData envEntriesMD) {
        if ((envEntriesMD == null) || (envEntriesMD.size() == 0)) {
            return Collections.emptyList();
        }

        final LinkedList<InjectionMetaData> retVal = new LinkedList<InjectionMetaData>();

        Set<ResourceInjectionTargetMetaData> injectionTargets;
        String envEntryName;
        String envEntryValue;
        String targetClass;
        String targetName;
        String envEntryValueClass;
        boolean hasInjectionTargets;

        // iterate through defined environment entries
        for (final EnvironmentEntryMetaData envEntryMD : envEntriesMD) {
            injectionTargets = envEntryMD.getInjectionTargets();
            hasInjectionTargets = (injectionTargets != null) && (injectionTargets.size() > 0);

            if (hasInjectionTargets) {
                // prepare env entry meta data
                envEntryName = envEntryMD.getEnvEntryName();
                envEntryValue = envEntryMD.getValue();
                envEntryValueClass = envEntryMD.getType();

                // env entry can specify multiple injection targets
                for (final ResourceInjectionTargetMetaData resourceInjectionTargetMD : injectionTargets) {
                    // prepare injection target meta data
                    targetClass = resourceInjectionTargetMD.getInjectionTargetClass();
                    targetName = resourceInjectionTargetMD.getInjectionTargetName();

                    // build injection meta data for injection target
                    final InjectionMetaData injectionMD = new InjectionMetaData(targetClass, targetName, envEntryValueClass,
                            envEntryName, envEntryValue != null);
                    this.log.debug(injectionMD);
                    retVal.add(injectionMD);
                }
            }
        }

        return retVal;
    }
}
