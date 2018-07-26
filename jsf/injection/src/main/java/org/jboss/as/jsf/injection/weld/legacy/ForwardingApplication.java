/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jsf.injection.weld.legacy;

import javax.el.ELContextListener;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.Behavior;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.faces.flow.FlowHandler;

/**
 * @author pmuir
 *
 * Bring this class back to allow JSF 1.2 to be used with WildFly
 * See https://issues.jboss.org/browse/WFLY-9708
 */
@SuppressWarnings({"deprecation"})
public abstract class ForwardingApplication extends Application {

    protected abstract Application delegate();

    public void addBehavior(String behaviorId, String behaviorClass) {
        delegate().addBehavior(behaviorId, behaviorClass);
    }

    public void addComponent(String componentType, String componentClass) {
        delegate().addComponent(componentType, componentClass);
    }

    public void addConverter(String converterId, String converterClass) {
        delegate().addConverter(converterId, converterClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addConverter(Class targetClass, String converterClass) {
        delegate().addConverter(targetClass, converterClass);
    }

    @Override
    public void addDefaultValidatorId(String validatorId) {
        delegate().addDefaultValidatorId(validatorId);
    }

    @Override
    public void addELContextListener(ELContextListener listener) {
        delegate().addELContextListener(listener);
    }

    @Override
    public void addELResolver(ELResolver resolver) {
        delegate().addELResolver(resolver);
    }

    @Override
    public void addValidator(String validatorId, String validatorClass) {
        delegate().addValidator(validatorId, validatorClass);
    }

    @Override
    public Behavior createBehavior(String behaviorId) throws FacesException {
        return delegate().createBehavior(behaviorId);
    }

    @Override
    public UIComponent createComponent(FacesContext context, Resource componentResource) {
        return delegate().createComponent(context, componentResource);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String componentType, String rendererType) {
        return delegate().createComponent(context, componentType, rendererType);
    }

    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType)
            throws FacesException {
        return delegate().createComponent(componentExpression, context, componentType);
    }

    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType,
            String rendererType) {
        return delegate().createComponent(componentExpression, context, componentType, rendererType);
    }

    @Override
    public UIComponent createComponent(String componentType) throws FacesException {
        return delegate().createComponent(componentType);
    }

    @Override
    @Deprecated
    public UIComponent createComponent(ValueBinding componentBinding, FacesContext context, String componentType)
            throws FacesException {
        return delegate().createComponent(componentBinding, context, componentType);
    }

    @Override
    public Converter createConverter(String converterId) {
        return delegate().createConverter(converterId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Converter createConverter(Class targetClass) {
        return delegate().createConverter(targetClass);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Override
    public MethodBinding createMethodBinding(String ref, Class[] params) throws ReferenceSyntaxException {
        return delegate().createMethodBinding(ref, params);
    }

    @Override
    public Validator createValidator(String validatorId) throws FacesException {
        return delegate().createValidator(validatorId);
    }

    @Override
    @Deprecated
    public ValueBinding createValueBinding(String ref) throws ReferenceSyntaxException {
        return delegate().createValueBinding(ref);
    }

    @Override
    public <T> T evaluateExpressionGet(FacesContext context, String expression, Class<? extends T> expectedType)
            throws ELException {
        return delegate().evaluateExpressionGet(context, expression, expectedType);
    }

    @Override
    public ActionListener getActionListener() {
        return delegate().getActionListener();
    }

    @Override
    public Iterator<String> getBehaviorIds() {
        return delegate().getBehaviorIds();
    }

    @Override
    public Iterator<String> getComponentTypes() {
        return delegate().getComponentTypes();
    }

    @Override
    public Iterator<String> getConverterIds() {
        return delegate().getConverterIds();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Class<?>> getConverterTypes() {
        return delegate().getConverterTypes();
    }

    @Override
    public Locale getDefaultLocale() {
        return delegate().getDefaultLocale();
    }

    @Override
    public String getDefaultRenderKitId() {
        return delegate().getDefaultRenderKitId();
    }

    @Override
    public Map<String, String> getDefaultValidatorInfo() {
        return delegate().getDefaultValidatorInfo();
    }

    @Override
    public ELContextListener[] getELContextListeners() {
        return delegate().getELContextListeners();
    }

    @Override
    public ELResolver getELResolver() {
        return delegate().getELResolver();
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return delegate().getExpressionFactory();
    }

    @Override
    public FlowHandler getFlowHandler() {
        return delegate().getFlowHandler();
    }

    @Override
    public String getMessageBundle() {
        return delegate().getMessageBundle();
    }

    @Override
    public NavigationHandler getNavigationHandler() {
        return delegate().getNavigationHandler();
    }

    @Override
    @Deprecated
    public PropertyResolver getPropertyResolver() {
        return delegate().getPropertyResolver();
    }

    @Override
    public ProjectStage getProjectStage() {
        return delegate().getProjectStage();
    }

    @Override
    public ResourceBundle getResourceBundle(FacesContext ctx, String name) {
        return delegate().getResourceBundle(ctx, name);
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return delegate().getResourceHandler();
    }

    @Override
    public StateManager getStateManager() {
        return delegate().getStateManager();
    }

    @Override
    public Iterator<Locale> getSupportedLocales() {
        return delegate().getSupportedLocales();
    }

    @Override
    public Iterator<String> getValidatorIds() {
        return delegate().getValidatorIds();
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return delegate().getVariableResolver();
    }

    @Override
    public ViewHandler getViewHandler() {
        return delegate().getViewHandler();
    }

    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Class<?> sourceBaseType,
            Object source) {
        delegate().publishEvent(context, systemEventClass, sourceBaseType, source);
    }

    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Object source) {
        delegate().publishEvent(context, systemEventClass, source);
    }

    @Override
    public void removeELContextListener(ELContextListener listener) {
        delegate().removeELContextListener(listener);
    }

    @Override
    public void setActionListener(ActionListener listener) {
        delegate().setActionListener(listener);
    }

    @Override
    public void setDefaultLocale(Locale locale) {
        delegate().setDefaultLocale(locale);
    }

    @Override
    public void setDefaultRenderKitId(String renderKitId) {
        delegate().setDefaultRenderKitId(renderKitId);
    }

    @Override
    public void setFlowHandler(FlowHandler newHandler) {
        delegate().setFlowHandler(newHandler);
    }

    @Override
    public void setMessageBundle(String bundle) {
        delegate().setMessageBundle(bundle);
    }

    @Override
    public void setNavigationHandler(NavigationHandler handler) {
        delegate().setNavigationHandler(handler);
    }

    @Override
    @Deprecated
    public void setPropertyResolver(PropertyResolver resolver) {
        delegate().setPropertyResolver(resolver);
    }

    @Override
    public void setResourceHandler(ResourceHandler resourceHandler) {
        delegate().setResourceHandler(resourceHandler);
    }

    @Override
    public void setStateManager(StateManager manager) {
        delegate().setStateManager(manager);
    }

    @Override
    public void setSupportedLocales(Collection<Locale> locales) {
        delegate().setSupportedLocales(locales);

    }

    @Override
    @Deprecated
    public void setVariableResolver(VariableResolver resolver) {
        delegate().setVariableResolver(resolver);
    }

    @Override
    public void setViewHandler(ViewHandler handler) {
        delegate().setViewHandler(handler);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
            SystemEventListener listener) {
        delegate().subscribeToEvent(systemEventClass, sourceClass, listener);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        delegate().subscribeToEvent(systemEventClass, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
            SystemEventListener listener) {
        delegate().unsubscribeFromEvent(systemEventClass, sourceClass, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        delegate().unsubscribeFromEvent(systemEventClass, listener);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

}