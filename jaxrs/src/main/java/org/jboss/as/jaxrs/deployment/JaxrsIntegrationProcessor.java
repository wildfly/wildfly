package org.jboss.as.jaxrs.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;
import javax.ws.rs.ApplicationPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.jaxrs");
    public static final String CDI_INJECTOR_FACTORY_CLASS = "org.jboss.resteasy.cdi.CdiInjectorFactory";
    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "javax.ws.rs.Application";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);


        if (resteasy == null)
            return;

        final Map<ModuleIdentifier, ResteasyDeploymentData> attachmentMap = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        final List<ResteasyDeploymentData> additionalData = new ArrayList<ResteasyDeploymentData>();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec != null && attachmentMap != null) {
            for (ModuleDependency dep : moduleSpec.getAllDependencies()) {
                if (attachmentMap.containsKey(dep.getIdentifier())) {
                    additionalData.add(attachmentMap.get(dep.getIdentifier()));
                }
            }
            resteasy.merge(additionalData);
        }
        if (!resteasy.getScannedResourceClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedResourceClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String resources = buf.toString();
            log.debug("Adding JAX-RS resource classes: " + resources);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, resources);
        }
        if (!resteasy.getScannedProviderClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String provider : resteasy.getScannedProviderClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(provider);
                } else {
                    buf.append(",").append(provider);
                }
            }
            String providers = buf.toString();
            log.debug("Adding JAX-RS provider classes: " + providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, providers);
        }

        if (!resteasy.getScannedJndiComponentResources().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedJndiComponentResources()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String providers = buf.toString();
            log.debug("Adding JAX-RS jndi component resource classes: " + providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
        }


        try {
            module.getClassLoader().loadClass(CDI_INJECTOR_FACTORY_CLASS);
            // don't set this param if CDI is not in classpath
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                log.debug("Found CDI, adding injector factory class");
                setContextParameter(webdata, "resteasy.injector.factory", CDI_INJECTOR_FACTORY_CLASS);
                //now we need to add the CDI extension, if it has not
                //already been added
                synchronized (parent) {
                    boolean found = false;
                    final List<Metadata<Extension>> extensions = parent.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);
                    for (Metadata<Extension> extension : extensions) {
                        if (extension.getValue() instanceof ResteasyCdiExtension) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {

                        final ClassLoader classLoader = SecurityActions.getContextClassLoader();
                        try {
                            //MASSIVE HACK
                            //the resteasy Logger throws a NPE if the TCCL is null
                            SecurityActions.setContextClassLoader(ResteasyCdiExtension.class.getClassLoader());
                            final ResteasyCdiExtension ext = new ResteasyCdiExtension();
                            Metadata<Extension> metadata = new Metadata<Extension>() {
                                @Override
                                public Extension getValue() {
                                    return ext;
                                }

                                @Override
                                public String getLocation() {
                                    return "org.jboss.as.jaxrs.JaxrsExtension";
                                }
                            };
                            parent.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
                        } finally {
                            SecurityActions.setContextClassLoader(classLoader);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        if (!resteasy.isUnwrappedExceptionsParameterSet()) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS, "javax.ejb.EJBException");
        }

        if (resteasy.hasBootClasses() || resteasy.isDispatcherCreated())
            return;

        //if there are no JAX-RS classes in the app just return
        if (resteasy.getScannedApplicationClass() == null
                && resteasy.getScannedJndiComponentResources().isEmpty()
                && resteasy.getScannedProviderClasses().isEmpty()
                && resteasy.getScannedResourceClasses().isEmpty()) return;

        boolean useScannedClass = false;
        String servletName;
        if (resteasy.getScannedApplicationClass() == null) {
            //if there is no scanned application we must add a servlet with a name of
            //javax.ws.rs.core.Application
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(JAX_RS_SERVLET_NAME);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            addServlet(webdata, servlet);
            servletName = JAX_RS_SERVLET_NAME;

        } else {
            //now there are two options.
            //if there is already a servlet defined with an init param
            //we don't do anything.
            //Otherwise we install our filter
            //JAVA-RS seems somewhat confused about the difference between a context param
            //and an init param.
            ParamValueMetaData param = findInitParam(webdata, SERVLET_INIT_PARAM);
            if (param != null) {
                //we need to promote the init param to a context param
                servletName = param.getParamValue();
                setContextParameter(webdata, "javax.ws.rs.Application", servletName);
            } else {
                ParamValueMetaData contextParam = findContextParam(webdata, "javax.ws.rs.Application");
                if (contextParam == null) {
                    setContextParameter(webdata, "javax.ws.rs.Application", resteasy.getScannedApplicationClass().getName());
                    useScannedClass = true;
                    servletName = resteasy.getScannedApplicationClass().getName();
                } else {
                    servletName = contextParam.getParamValue();
                }
            }
        }

        boolean mappingSet = false;

        if (useScannedClass) {
            //add a servlet named after the application class
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(servletName);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            addServlet(webdata, servlet);
            //look for mappings:
            if (!servletMappingsExist(webdata, servletName)) {
                //no mappings, add our own
                List<String> patterns = new ArrayList<String>();
                if (resteasy.getScannedApplicationClass().isAnnotationPresent(ApplicationPath.class)) {
                    ApplicationPath path = resteasy.getScannedApplicationClass().getAnnotation(ApplicationPath.class);
                    String pathValue = path.value().trim();
                    if (!pathValue.startsWith("/")) {
                        pathValue = "/" + pathValue;
                    }
                    String prefix = pathValue;
                    if (pathValue.endsWith("/")) {
                        pathValue += "*";
                    } else {
                        pathValue += "/*";
                    }
                    patterns.add(pathValue);
                    setContextParameter(webdata, "resteasy.servlet.mapping.prefix", prefix);
                    mappingSet = true;
                } else {
                    throw new DeploymentUnitProcessingException("No Servlet mappings found for JAX-RS application: " + servletName + " either annotate it with @ApplicationPath or add a servlet-mapping in web.xml");
                }
                ServletMappingMetaData mapping = new ServletMappingMetaData();
                mapping.setServletName(servletName);
                mapping.setUrlPatterns(patterns);
                if (webdata.getServletMappings() == null) {
                    webdata.setServletMappings(new ArrayList<ServletMappingMetaData>());
                }
                webdata.getServletMappings().add(mapping);
            }
        }

        if (!mappingSet) {
            //now we need tell resteasy it's relative path
            final List<ServletMappingMetaData> mappings = webdata.getServletMappings();
            if (mappings != null) {
                for (final ServletMappingMetaData mapping : mappings) {
                    if (mapping.getServletName().equals(servletName)) {
                        if (mapping.getUrlPatterns() != null) {
                            for (String pattern : mapping.getUrlPatterns()) {
                                if (mappingSet) {
                                    log.errorf("More than one mapping found for JAX-RS servlet: %s the second mapping %s will not work", servletName, pattern);
                                } else {
                                    mappingSet = true;
                                    String realPattern = pattern;
                                    if (realPattern.endsWith("*")) {
                                        realPattern = realPattern.substring(0, realPattern.length() - 1);
                                    }
                                    setContextParameter(webdata, "resteasy.servlet.mapping.prefix", realPattern);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void addServlet(JBossWebMetaData webdata, JBossServletMetaData servlet) {
        if (webdata.getServlets() == null) {
            webdata.setServlets(new JBossServletsMetaData());
        }
        webdata.getServlets().add(servlet);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    protected void setFilterInitParam(FilterMetaData filter, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = filter.getInitParam();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            filter.setInitParam(params);
        }
        params.add(param);

    }

    public static ParamValueMetaData findContextParam(JBossWebMetaData webdata, String name) {
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null)
            return null;
        for (ParamValueMetaData param : params) {
            if (param.getParamName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public static ParamValueMetaData findInitParam(JBossWebMetaData webdata, String name) {
        JBossServletsMetaData servlets = webdata.getServlets();
        if (servlets == null)
            return null;
        for (JBossServletMetaData servlet : servlets) {
            List<ParamValueMetaData> initParams = servlet.getInitParam();
            if (initParams != null) {
                for (ParamValueMetaData param : initParams) {
                    if (param.getParamName().equals(name)) {
                        return param;
                    }
                }
            }
        }
        return null;
    }

    public static boolean servletMappingsExist(JBossWebMetaData webdata, String servletName) {
        List<ServletMappingMetaData> mappings = webdata.getServletMappings();
        if (mappings == null)
            return false;
        for (ServletMappingMetaData mapping : mappings) {
            if (mapping.getServletName().equals(servletName)) {
                return true;
            }
        }
        return false;
    }


    public static void setContextParameter(JBossWebMetaData webdata, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }


}