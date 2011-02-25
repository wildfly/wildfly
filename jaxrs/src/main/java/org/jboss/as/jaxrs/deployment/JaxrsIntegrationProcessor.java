package org.jboss.as.jaxrs.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ApplicationPath;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.modules.Module;
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.jaxrs");
    public static final String CDI_INJECTOR_FACTORY_CLASS = "org.jboss.resteasy.cdi.CdiInjectorFactory";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if (resteasy == null)
            return;

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
            log.info("*** Adding JAX-RS resource classes: " + resources);
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
            log.info("*** Adding JAX-RS provider classes: " + providers);
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
            log.info("*** Adding JAX-RS jndi component resource classes: " + providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
        }

        boolean useScannedApplicationClass = false;

        if (resteasy.getScannedApplicationClass() != null) {
            if (findContextParam(webdata, "javax.ws.rs.Application") == null) {
                useScannedApplicationClass = true;
                setContextParameter(webdata, "javax.ws.rs.Application", resteasy.getScannedApplicationClass().getName());
            }
        }

        try {
            module.getClassLoader().loadClass(CDI_INJECTOR_FACTORY_CLASS);
            // don't set this param if CDI is not in classpath
            if (WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
                log.debug("***** Found CDI, adding injector factory class");
                setContextParameter(webdata, "resteasy.injector.factory", CDI_INJECTOR_FACTORY_CLASS);
            }
        } catch (ClassNotFoundException ignored) {
        }

        if (!resteasy.isUnwrappedExceptionsParameterSet()) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS, "javax.ejb.EJBException");
        }

        if (resteasy.hasBootClasses() || resteasy.isDispatcherCreated())
            return;
        if (resteasy.getScannedApplicationClass() == null && resteasy.getScannedJndiComponentResources().isEmpty()
                && resteasy.getScannedProviderClasses().isEmpty() && resteasy.getScannedResourceClasses().isEmpty())
            return;

        FilterMetaData filter = new FilterMetaData();
        filter.setFilterClass(Filter30Dispatcher.class.getName());
        filter.setName("Resteasy");
        filter.setAsyncSupported(true);

        FilterMappingMetaData mapping = new FilterMappingMetaData();
        mapping.setFilterName("Resteasy");
        List<String> patterns = new ArrayList<String>();
        if (useScannedApplicationClass && resteasy.getScannedApplicationClass().isAnnotationPresent(ApplicationPath.class)) {
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
        } else {
            patterns.add("/*");
        }
        mapping.setUrlPatterns(patterns);

        if (webdata.getFilters() == null) {
            webdata.setFilters(new FiltersMetaData());
        }
        webdata.getFilters().add(filter);
        List<FilterMappingMetaData> mappings = webdata.getFilterMappings();
        if (mappings == null) {
            mappings = new ArrayList<FilterMappingMetaData>();
            webdata.setFilterMappings(mappings);
        }
        mappings.add(mapping);
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