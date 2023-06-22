package org.jboss.as.jsf.deployment;

import java.util.ArrayList;
import java.util.List;

import jakarta.faces.application.ViewHandler;
import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;

/**
 * @author Stuart Douglas
 */
public class JSFMetadataProcessor implements DeploymentUnitProcessor {

    public static final String JAVAX_FACES_WEBAPP_FACES_SERVLET = "jakarta.faces.webapp.FacesServlet";
    private static final String DISALLOW_DOCTYPE_DECL = "com.sun.faces.disallowDoctypeDecl";
    private static final String LAZY_BEAN_VALIDATION_PARAM = "com.sun.faces.enableLazyBeanValidation";


    private static final int defaultBufferSize;

    // This is copied from org.wildfly.extension.undertow.ByteBufferPoolDefinition to come up with a decent default for
    // the jakarta.faces.FACELETS_BUFFER_SIZE property. We calculate this because the default is 1024, which is very
    // small, https://jakarta.ee/specifications/faces/4.0/jakarta-faces-4.0.html#a6088. Per the spec we could use -1
    // as the default, but Mojarra does not currently support that. Once https://github.com/eclipse-ee4j/mojarra/issues/5262
    // is resolved, and we can upgrade, we should be able to default to -1.
    static {
        long maxMemory = Runtime.getRuntime().maxMemory();
        //smaller than 64mb of ram we use 512b buffers
        if (maxMemory < 64 * 1024 * 1024) {
            //use 512b buffers
            defaultBufferSize = 512;
        } else if (maxMemory < 128 * 1024 * 1024) {
            //use 1k buffers
            defaultBufferSize = 1024;
        } else {
            //use 16k buffers for best performance
            //as 16k is generally the max amount of data that can be sent in a single write() call
            defaultBufferSize = 1024 * 16;
        }
    }

    private final Boolean disallowDoctypeDecl;

    public JSFMetadataProcessor(final Boolean disallowDoctypeDecl) {
        this.disallowDoctypeDecl = disallowDoctypeDecl;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }
        if(metaData == null || metaData.getMergedJBossWebMetaData() == null || metaData.getMergedJBossWebMetaData().getServlets() == null) {
            return;
        }
        JBossServletMetaData jsf = null;
        for(JBossServletMetaData servlet : metaData.getMergedJBossWebMetaData().getServlets()) {
            if(JAVAX_FACES_WEBAPP_FACES_SERVLET.equals(servlet.getServletClass())) {
                jsf = servlet;
            }
        }
        if (jsf != null && jsf.getMultipartConfig() == null) {
            // WFLY-2329 File upload doesn't work
            jsf.setMultipartConfig(new MultipartConfigMetaData());
        }
        if (disallowDoctypeDecl != null) {
            // Add the disallowDoctypeDecl context param if it's not already present
            setContextParameterIfAbsent(metaData.getMergedJBossWebMetaData(), DISALLOW_DOCTYPE_DECL, disallowDoctypeDecl.toString());
        }
        // Auto-disable lazy bean validation for distributable web application.
        // This can otherwise cause missing @PreDestroy events.
        if (metaData.getMergedJBossWebMetaData().getDistributable() != null) {
            String disabled = Boolean.toString(false);
            if (!setContextParameterIfAbsent(metaData.getMergedJBossWebMetaData(), LAZY_BEAN_VALIDATION_PARAM, disabled).equals(disabled)) {
                JSFLogger.ROOT_LOGGER.lazyBeanValidationEnabled();
            }
        }
        // Set a default buffer size as 1024 is too small
        final JBossWebMetaData webMetaData = metaData.getMergedJBossWebMetaData();
        // First check the legacy facelets.BUFFER_SIZE property which is required for backwards compatibility
        if (!hasContextParam(webMetaData, "facelets.BUFFER_SIZE")) {
            // The legacy parameter has not been set, set a default buffer if the current parameter name has not been set.
            setContextParameterIfAbsent(metaData.getMergedJBossWebMetaData(), ViewHandler.FACELETS_BUFFER_SIZE_PARAM_NAME, Integer.toString(defaultBufferSize));
        }
    }

    private static String setContextParameterIfAbsent(final JBossWebMetaData webMetaData, final String name, final String value) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
            webMetaData.setContextParams(contextParams);
        }
        for (ParamValueMetaData param : contextParams) {
            if (name.equals(param.getParamName()) && param.getParamValue() != null) {
                // already set
                return param.getParamValue();
            }
        }
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        contextParams.add(param);
        return value;
    }

    private static boolean hasContextParam(final JBossWebMetaData webMetaData, final String name) {
        final List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            return false;
        }
        return contextParams.stream()
                .anyMatch(value -> name.equals(value.getParamName()));
    }
}
