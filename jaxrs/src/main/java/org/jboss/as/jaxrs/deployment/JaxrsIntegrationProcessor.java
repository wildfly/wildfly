package org.jboss.as.jaxrs.deployment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ApplicationPath;

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
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import static org.jboss.as.jaxrs.JaxrsLogger.JAXRS_LOGGER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "javax.ws.rs.Application";
    public static final String RESTEASY_SCAN = "resteasy.scan";
    public static final String RESTEASY_SCAN_RESOURCES = "resteasy.scan.resources";
    public static final String RESTEASY_SCAN_PROVIDERS = "resteasy.scan.providers";

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

        //remove the resteasy.scan parameter
        //because it is not needed
        final List<ParamValueMetaData> params = webdata.getContextParams();
        if (params != null) {
            Iterator<ParamValueMetaData> it = params.iterator();
            while (it.hasNext()) {
                final ParamValueMetaData param = it.next();
                if (param.getParamName().equals(RESTEASY_SCAN)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN);
                } else if (param.getParamName().equals(RESTEASY_SCAN_RESOURCES)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN_RESOURCES);
                } else if (param.getParamName().equals(RESTEASY_SCAN_PROVIDERS)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN_PROVIDERS);
                }
            }
        }


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
            JAXRS_LOGGER.debugf("Adding JAX-RS resource classes: %s", resources);
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
            JAXRS_LOGGER.debugf("Adding JAX-RS provider classes: %s", providers);
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
            JAXRS_LOGGER.debugf("Adding JAX-RS jndi component resource classes: %s", providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
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
            servlet.setAsyncSupported(true);
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

            //look for servlet mappings
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
                    JAXRS_LOGGER.noServletMappingFound(servletName);
                    return;
                }
                ServletMappingMetaData mapping = new ServletMappingMetaData();
                mapping.setServletName(servletName);
                mapping.setUrlPatterns(patterns);
                if (webdata.getServletMappings() == null) {
                    webdata.setServletMappings(new ArrayList<ServletMappingMetaData>());
                }
                webdata.getServletMappings().add(mapping);
            }

            //add a servlet named after the application class
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(servletName);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            addServlet(webdata, servlet);

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
                                    JAXRS_LOGGER.moreThanOneServletMapping(servletName, pattern);
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