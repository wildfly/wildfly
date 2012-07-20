package org.jboss.as.server.deploymentoverlay.service;

import org.jboss.as.repository.ContentRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

/**
 * @author Stuart Douglas
 */
public class ContentService implements Service<ContentService> {

    public static final ServiceName SERVICE_NAME = DeploymentOverlayService.SERVICE_NAME.append("contentService");

    private final InjectedValue<ContentRepository> contentRepositoryInjectedValue = new InjectedValue<ContentRepository>();
    private final InjectedValue<DeploymentOverlayService> deploymentOverlayServiceInjectedValue = new InjectedValue<DeploymentOverlayService>();

    private final String path;
    private final byte[] contentHash;

    public ContentService(final String path, final byte[] contentHash) {
        this.path = path;
        this.contentHash = contentHash;
    }


    @Override
    public void start(final StartContext context) throws StartException {
        deploymentOverlayServiceInjectedValue.getValue().addContentService(this);
    }

    @Override
    public void stop(final StopContext context) {
        deploymentOverlayServiceInjectedValue.getValue().removeContentService(this);
    }

    @Override
    public ContentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public VirtualFile getContentHash() {
        return contentRepositoryInjectedValue.getValue().getContent(contentHash);
    }

    public String getPath() {
        return path;
    }

    public InjectedValue<ContentRepository> getContentRepositoryInjectedValue() {
        return contentRepositoryInjectedValue;
    }

    public InjectedValue<DeploymentOverlayService> getDeploymentOverlayServiceInjectedValue() {
        return deploymentOverlayServiceInjectedValue;
    }
}
