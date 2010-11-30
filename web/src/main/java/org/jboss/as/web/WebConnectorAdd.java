/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Update adding a web connector.
 *
 * @author Emanuel Muckenhuber
 */
public class WebConnectorAdd extends AbstractWebSubsystemUpdate<Void> {

    private static final long serialVersionUID = 8619926322146139691L;

    private final String name;
    private String protocol;
    private String bindingRef;
    private String scheme;
    private String executorRef;
    private Boolean enabled;
    private Boolean enableLookups;
    private String proxyName;
    private Integer proxyPort;
    private Integer redirectPort;
    private Boolean secure;
    private Integer maxPostSize;
    private Integer maxSavePostSize;

    public WebConnectorAdd(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null connector name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(WebSubsystemElement element) throws UpdateFailedException {
        final WebConnectorElement connector = element.addConnector(name);
        if(connector == null) {
            throw new UpdateFailedException("duplicate connector " + name);
        }
        connector.setProtocol(protocol);
        connector.setBindingRef(bindingRef);
        connector.setScheme(scheme);
        connector.setExecutorRef(executorRef);
        if(enabled != null) connector.setEnabled(enabled);
        if(secure != null) connector.setSecure(secure);
        if(enableLookups != null) connector.setEnableLookups(enabled);
        if(proxyName != null) connector.setProxyName(proxyName);
        if(proxyPort != null) connector.setProxyPort(proxyPort);
        if(redirectPort != null) connector.setRedirectPort(redirectPort);
        if(maxPostSize != null) connector.setMaxPostSize(maxPostSize);
        if(maxSavePostSize != null) connector.setMaxSavePostSize(maxSavePostSize);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final boolean enabled = this.enabled == null || this.enabled;
        final WebConnectorService service = new WebConnectorService(protocol, scheme);
        if(secure != null) service.setSecure(secure);
        if(enableLookups != null) service.setEnableLookups(enabled);
        if(proxyName != null) service.setProxyName(proxyName);
        if(proxyPort != null) service.setProxyPort(proxyPort);
        if(redirectPort != null) service.setRedirectPort(redirectPort);
        if(maxPostSize != null) service.setMaxPostSize(maxPostSize);
        if(maxSavePostSize != null) service.setMaxSavePostSize(maxSavePostSize);
        context.getBatchBuilder().addService(WebSubsystemElement.JBOSS_WEB_CONNECTOR.append(name), service)
            .addDependency(WebSubsystemElement.JBOSS_WEB, WebServer.class, service.getServer())
            .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding())
            .addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param))
            .setInitialMode(enabled ? Mode.ACTIVE : Mode.NEVER)
            .install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<WebSubsystemElement, ?> getCompensatingUpdate(WebSubsystemElement original) {
        return new WebConnectorRemove(name);
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getBindingRef() {
        return bindingRef;
    }

    public void setBindingRef(String bindingRef) {
        this.bindingRef = bindingRef;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getExecutorRef() {
        return executorRef;
    }

    public void setExecutorRef(String executorRef) {
        this.executorRef = executorRef;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    protected Boolean getEnableLookups() {
        return enableLookups;
    }

    protected void setEnableLookups(Boolean enableLookups) {
        this.enableLookups = enableLookups;
    }

    protected String getProxyName() {
        return proxyName;
    }

    protected void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    protected Integer getProxyPort() {
        return proxyPort;
    }

    protected void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    protected Integer getRedirectPort() {
        return redirectPort;
    }

    protected void setRedirectPort(Integer redirectPort) {
        this.redirectPort = redirectPort;
    }

    protected Integer getMaxPostSize() {
        return maxPostSize;
    }

    protected void setMaxPostSize(Integer maxPostSize) {
        this.maxPostSize = maxPostSize;
    }

    protected Integer getMaxSavePostSize() {
        return maxSavePostSize;
    }

    protected void setMaxSavePostSize(Integer maxSavePostSize) {
        this.maxSavePostSize = maxSavePostSize;
    }

    static WebConnectorAdd create(final WebConnectorElement connector) {
        final WebConnectorAdd action = new WebConnectorAdd(connector.getName());
        action.setBindingRef(connector.getBindingRef());
        action.setProtocol(connector.getProtocol());
        action.setScheme(connector.getScheme());
        action.setExecutorRef(connector.getExecutorRef());
        action.setEnabled(connector.isEnabled());
        action.setSecure(connector.isSecure());
        action.setEnableLookups(connector.isEnableLookups());
        action.setProxyName(connector.getProxyName());
        action.setProxyPort(connector.getProxyPort());
        action.setRedirectPort(connector.getRedirectPort());
        action.setMaxPostSize(connector.getMaxPostSize());
        action.setMaxSavePostSize(connector.getMaxSavePostSize());
        return action;
    }

}
