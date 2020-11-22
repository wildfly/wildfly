/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jaxrs;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsServerConfig {

    private ModelNode jaxrs20RequestMatching;
    private ModelNode resteasyAddCharset;
    private ModelNode resteasyBufferExceptionEntity;
    private ModelNode resteasyDisableHtmlSanitizer;
    private ModelNode resteasyDisableProviders;
    private ModelNode resteasyDocumentExpandEntityReferences;
    private ModelNode resteasyDocumentSecureProcessingFeature;
    private ModelNode resteasyGzipMaxInput;
    private ModelNode resteasyJndiResources;
    private ModelNode resteasyLanguageMappings;
    private ModelNode resteasyMediaTypeMappings;
    private ModelNode resteasyMediaTypeParamMapping;
    private ModelNode resteasyOriginalWebApplicationExceptionBehavior;
    private ModelNode resteasyPreferJacksonOverJsonB;
    private ModelNode resteasyProviders;
    private ModelNode resteasyRFC7232Preconditions;
    private ModelNode resteasyRoleBasedSecurity;
    private ModelNode resteasySecureDisableDTDs;
    private ModelNode resteasySecureRandomMaxUse;
    private ModelNode resteasyUseBuiltinProviders;
    private ModelNode resteasyUseContainerFormParams;
    private ModelNode resteasyWiderRequestMatching;

    public ModelNode isJaxrs20RequestMatching() {
        return jaxrs20RequestMatching;
    }
    public void setJaxrs20RequestMatching(ModelNode jaxrs20RequestMatching) {
        this.jaxrs20RequestMatching = jaxrs20RequestMatching;
    }
    public ModelNode isResteasyAddCharset() {
        return resteasyAddCharset;
    }
    public void setResteasyAddCharset(ModelNode resteasyAddCharset) {
        this.resteasyAddCharset = resteasyAddCharset;
    }
    public ModelNode isResteasyBufferExceptionEntity() {
        return resteasyBufferExceptionEntity;
    }
    public void setResteasyBufferExceptionEntity(ModelNode resteasyBufferExceptionEntity) {
        this.resteasyBufferExceptionEntity = resteasyBufferExceptionEntity;
    }
    public ModelNode isResteasyDisableHtmlSanitizer() {
        return resteasyDisableHtmlSanitizer;
    }
    public void setResteasyDisableHtmlSanitizer(ModelNode resteasyDisableHtmlSanitizer) {
        this.resteasyDisableHtmlSanitizer = resteasyDisableHtmlSanitizer;
    }
    public ModelNode getResteasyDisableProviders() {
        return resteasyDisableProviders;
    }
    public void setResteasyDisableProviders(ModelNode resteasyDisableProviders) {
        this.resteasyDisableProviders = resteasyDisableProviders;
    }
    public ModelNode isResteasyDocumentExpandEntityReferences() {
        return resteasyDocumentExpandEntityReferences;
    }
    public void setResteasyDocumentExpandEntityReferences(ModelNode resteasyDocumentExpandEntityReferences) {
        this.resteasyDocumentExpandEntityReferences = resteasyDocumentExpandEntityReferences;
    }
    public ModelNode isResteasyDocumentSecureProcessingFeature() {
        return resteasyDocumentSecureProcessingFeature;
    }
    public void setResteasyDocumentSecureProcessingFeature(ModelNode resteasyDocumentSecureProcessingFeature) {
        this.resteasyDocumentSecureProcessingFeature = resteasyDocumentSecureProcessingFeature;
    }
    public ModelNode getResteasyGzipMaxInput() {
        return resteasyGzipMaxInput;
    }
    public void setResteasyGzipMaxInput(ModelNode resteasyGzipMaxInput) {
        this.resteasyGzipMaxInput = resteasyGzipMaxInput;
    }
    public ModelNode getResteasyJndiResources() {
        return resteasyJndiResources;
    }
    public void setResteasyJndiResources(ModelNode resteasyJndiResources) {
        this.resteasyJndiResources = resteasyJndiResources;
    }
    public ModelNode getResteasyLanguageMappings() {
        return resteasyLanguageMappings;
    }
    public void setResteasyLanguageMappings(ModelNode resteasyLanguageMappings) {
        this.resteasyLanguageMappings = resteasyLanguageMappings;
    }
    public ModelNode getResteasyMediaTypeMappings() {
        return resteasyMediaTypeMappings;
    }
    public void setResteasyMediaTypeMappings(ModelNode resteasyMediaTypeMappings) {
        this.resteasyMediaTypeMappings = resteasyMediaTypeMappings;
    }
    public ModelNode getResteasyMediaTypeParamMapping() {
        return resteasyMediaTypeParamMapping;
    }
    public ModelNode getResteasyOriginalWebApplicationExceptionBehavior() {
        return resteasyOriginalWebApplicationExceptionBehavior;
    }
    public void setResteasyOriginalWebApplicationExceptionBehavior(ModelNode resteasyOriginalWebApplicationExceptionBehavior) {
        this.resteasyOriginalWebApplicationExceptionBehavior = resteasyOriginalWebApplicationExceptionBehavior;
    }
    public void setResteasyMediaTypeParamMapping(ModelNode resteasyMediaTypeParamMapping) {
        this.resteasyMediaTypeParamMapping = resteasyMediaTypeParamMapping;
    }
    public ModelNode isResteasyPreferJacksonOverJsonB() {
        return resteasyPreferJacksonOverJsonB;
    }
    public void setResteasyPreferJacksonOverJsonB(ModelNode resteasyPreferJacksonOverJsonB) {
        this.resteasyPreferJacksonOverJsonB = resteasyPreferJacksonOverJsonB;
    }
    public ModelNode getResteasyProviders() {
        return resteasyProviders;
    }
    public void setResteasyProviders(ModelNode resteasyProviders) {
        this.resteasyProviders = resteasyProviders;
    }
    public ModelNode isResteasyRFC7232Preconditions() {
        return resteasyRFC7232Preconditions;
    }
    public void setResteasyRFC7232Preconditions(ModelNode resteasyRFC7232Preconditions) {
        this.resteasyRFC7232Preconditions = resteasyRFC7232Preconditions;
    }
    public ModelNode isResteasyRoleBasedSecurity() {
        return resteasyRoleBasedSecurity;
    }
    public void setResteasyRoleBasedSecurity(ModelNode resteasyRoleBasedSecurity) {
        this.resteasyRoleBasedSecurity = resteasyRoleBasedSecurity;
    }
    public ModelNode isResteasySecureDisableDTDs() {
        return resteasySecureDisableDTDs;
    }
    public void setResteasySecureDisableDTDs(ModelNode resteasySecureDisableDTDs) {
        this.resteasySecureDisableDTDs = resteasySecureDisableDTDs;
    }
    public ModelNode getResteasySecureRandomMaxUse() {
        return resteasySecureRandomMaxUse;
    }
    public void setResteasySecureRandomMaxUse(ModelNode resteasySecureRandomMaxUse) {
        this.resteasySecureRandomMaxUse = resteasySecureRandomMaxUse;
    }
    public ModelNode isResteasyUseBuiltinProviders() {
        return resteasyUseBuiltinProviders;
    }
    public void setResteasyUseBuiltinProviders(ModelNode resteasyUseBuiltinProviders) {
        this.resteasyUseBuiltinProviders = resteasyUseBuiltinProviders;
    }
    public ModelNode isResteasyUseContainerFormParams() {
        return resteasyUseContainerFormParams;
    }
    public void setResteasyUseContainerFormParams(ModelNode resteasyUseContainerFormParams) {
        this.resteasyUseContainerFormParams = resteasyUseContainerFormParams;
    }
    public ModelNode isResteasyWiderRequestMatching() {
        return resteasyWiderRequestMatching;
    }
    public void setResteasyWiderRequestMatching(ModelNode resteasyWiderRequestMatching) {
        this.resteasyWiderRequestMatching = resteasyWiderRequestMatching;
    }
}
