package org.jboss.as.jsf.deployment;

import java.util.ArrayList;
import java.util.List;

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

    public static final String JAVAX_FACES_WEBAPP_FACES_SERVLET = "javax.faces.webapp.FacesServlet";
    private static final String DISALLOW_DOCTYPE_DECL = "com.sun.faces.disallowDoctypeDecl";

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
        if(jsf != null) {
            if(jsf.getMultipartConfig() == null) {
                //WFLY-2329 File upload doesn't work
                jsf.setMultipartConfig(new MultipartConfigMetaData());
            }
        }
        if (disallowDoctypeDecl != null) {
            // Add the disallowDoctypeDecl context param if it's not already present
            setContextParameterIfAbsent(metaData.getMergedJBossWebMetaData(), DISALLOW_DOCTYPE_DECL, disallowDoctypeDecl.toString());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void setContextParameterIfAbsent(final JBossWebMetaData webMetaData, final String name, final String value) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
            webMetaData.setContextParams(contextParams);
        }
        for (ParamValueMetaData param : contextParams) {
            if (name.equals(param.getParamName()) && param.getParamValue() != null) {
                // already set
                return;
            }
        }
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        contextParams.add(param);
    }
}
