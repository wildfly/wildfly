package org.jboss.as.jsf.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;

/**
 * @author Stuart Douglas
 */
public class JSFMetadataProcessor implements DeploymentUnitProcessor {

    public static final String JAVAX_FACES_WEBAPP_FACES_SERVLET = "javax.faces.webapp.FacesServlet";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(metaData == null || metaData.getMergedJBossWebMetaData() == null || metaData.getMergedJBossWebMetaData().getServlets() == null) {
            return;
        }
        JBossServletMetaData jsf = null;
        for(JBossServletMetaData servlet : metaData.getMergedJBossWebMetaData().getServlets()) {
            if(servlet.getServletClass().equals(JAVAX_FACES_WEBAPP_FACES_SERVLET)) {
                jsf = servlet;
            }
        }
        if(jsf != null) {
            if(jsf.getMultipartConfig() == null) {
                //WFLY-2329 File upload doesn't work
                jsf.setMultipartConfig(new MultipartConfigMetaData());
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
