/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.webservices.deployers;

import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * A DUP that sets the dependencies required for using WS classes in WS deployments
 *
 * @author alessio.soldano@jboss.com
 * @since 19-Jan-2011
 */
public class WSDependenciesProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier APACHE_CXF = ModuleIdentifier.create("org.apache.cxf");
    private static final ModuleIdentifier APACHE_XALAN = ModuleIdentifier.create("org.apache.xalan");
    private static final ModuleIdentifier APACHE_XERCES = ModuleIdentifier.create("org.apache.xerces");
    private static final ModuleIdentifier JAXB_IMPL = ModuleIdentifier.create("com.sun.xml.bind");
    private static final ModuleIdentifier JBOSS_WS_COMMON = ModuleIdentifier.create("org.jboss.ws.common");
    private static final ModuleIdentifier JBOSS_WS_SPI = ModuleIdentifier.create("org.jboss.ws.spi");
    private static final ModuleIdentifier JBOSS_WS_CXF_CLIENT = ModuleIdentifier.create("org.jboss.ws.cxf.jbossws-cxf-client");
    private static final ModuleIdentifier JBOSS_WS_CXF_FACTORIES = ModuleIdentifier.create("org.jboss.ws.cxf.jbossws-cxf-factories");
    private static final ModuleIdentifier JBOSS_WS_CXF_SERVER = ModuleIdentifier.create("org.jboss.ws.cxf.jbossws-cxf-server");
    private static final ModuleIdentifier JBOSS_WS_CXF_TRANSPORTS_HTTPSERVER = ModuleIdentifier.create("org.jboss.ws.cxf.jbossws-cxf-transports-httpserver");
    private static final ModuleIdentifier JBOSS_WEBSERVICES = ModuleIdentifier.create("org.jboss.as.webservices");
    private static final ModuleIdentifier SAAJ_IMPL = ModuleIdentifier.create("com.sun.xml.messaging.saaj");
    private static final ModuleIdentifier WSDL4J = ModuleIdentifier.create("wsdl4j.wsdl4j");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWSDeployment(deploymentUnit)) {
            final ModuleLoader moduleLoader = Module.getSystemModuleLoader();
            final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

            // FIXME see if/how we can or should avoid (or at least limit) exposing the whole server stack code
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JAXB_IMPL, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WS_SPI, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WS_COMMON, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WS_CXF_CLIENT, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WS_CXF_FACTORIES, false, false, true));
            moduleSpecification.addDependency(applyCXFExtensionImportFilters(new ModuleDependency(moduleLoader, JBOSS_WS_CXF_TRANSPORTS_HTTPSERVER, false, false, true)));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WS_CXF_SERVER, false, false, true));

            moduleSpecification.addDependency(applyCXFExtensionImportFilters(new ModuleDependency(moduleLoader, APACHE_CXF, false, false, true)));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, APACHE_XALAN, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, APACHE_XERCES, false, false, true));

            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JBOSS_WEBSERVICES, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, SAAJ_IMPL, false, false, true));
            moduleSpecification.addDependency(new ModuleDependency(moduleLoader, WSDL4J, false, false, true));
        }
    }

    private static ModuleDependency applyCXFExtensionImportFilters(ModuleDependency dep) {
        dep.getImportFilters().add(new FilterSpecification(PathFilters.match("META-INF/cxf"), true)); //to include bus extensions in META-INF
        dep.getImportFilters().add(new FilterSpecification(PathFilters.match("META-INF/spring.*"), true));
        return dep;
    }

    /**
     * Look for @WebService / @WebServiceProvider annotated classes only as the jbossweb metadata is not available yet here
     * TODO: to be improved (jaxrpc deployments are skipped)
     *
     * @param unit
     * @return
     */
    private boolean isWSDeployment(DeploymentUnit unit) {
        final DotName webserviceProviderAnnotation = DotName.createSimple(WebServiceProvider.class.getName());
        final Index index = ASHelper.getRootAnnotationIndex(unit);
        if (index == null) {
            // this should only happen with ear modules
            return false;
        }
        final List<AnnotationInstance> wsProvAnnList = index.getAnnotations(webserviceProviderAnnotation);

        return hasWebServiceImpl(index) || (wsProvAnnList != null && wsProvAnnList.size() > 0);
    }

    private boolean hasWebServiceImpl(Index index) {
        final DotName webserviceAnnotation = DotName.createSimple(WebService.class.getName());
        final List<AnnotationInstance> wsAnnList = index.getAnnotations(webserviceAnnotation);
        if (wsAnnList != null) {
            for (AnnotationInstance ai : wsAnnList) {
                AnnotationTarget target = ai.target();
                if (target instanceof ClassInfo) {
                    short flags = ((ClassInfo) target).flags();
                    // Interfaces have 0x200 and 0x400 flags ON
                    // http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
                    if (flags < 0x600) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
