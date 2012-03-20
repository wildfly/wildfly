/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jsf.jta.customui;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.ValueChangeListener;
import javax.faces.render.Renderer;
import javax.faces.validator.Validator;

import org.jboss.as.test.integration.jsf.jta.CustomUITestCase;


/**
 * @author baranowb
 * 
 */
@SuppressWarnings("deprecation")
public class HelloUIComponent extends UIInput {

    public void encodeBegin(FacesContext context) throws IOException {
        CustomUITestCase.doLookupTest();
        super.encodeBegin(context);
    }

    public String getFamily() {
        CustomUITestCase.doLookupTest();
        return "HelloKittyFamily";
    }

    @Override
    public Map<String, Object> getAttributes() {
        CustomUITestCase.doLookupTest();
        return super.getAttributes();
    }
    
    @Override
    public ValueBinding getValueBinding(String name) {
        CustomUITestCase.doLookupTest();
        return super.getValueBinding(name);
    }

    @Override
    public void setValueBinding(String name, ValueBinding binding) {
        CustomUITestCase.doLookupTest();
        super.setValueBinding(name, binding);
    }

    @Override
    public String getClientId(FacesContext context) {
        CustomUITestCase.doLookupTest();
        return super.getClientId(context);
    }

    @Override
    public String getId() {
        CustomUITestCase.doLookupTest();
        return super.getId();
    }

    @Override
    public void setId(String id) {
        CustomUITestCase.doLookupTest();
        super.setId(id);
    }

    @Override
    public UIComponent getParent() {
        CustomUITestCase.doLookupTest();
        return super.getParent();
    }

    @Override
    public void setParent(UIComponent parent) {
        CustomUITestCase.doLookupTest();
        super.setParent(parent);
    }

    @Override
    public boolean isRendered() {
        CustomUITestCase.doLookupTest();
        return super.isRendered();
    }

    @Override
    public void setRendered(boolean rendered) {
        CustomUITestCase.doLookupTest();
        super.setRendered(rendered);
    }

    @Override
    public String getRendererType() {
        CustomUITestCase.doLookupTest();
        return super.getRendererType();
    }

    @Override
    public void setRendererType(String rendererType) {
        CustomUITestCase.doLookupTest();
        super.setRendererType(rendererType);
    }

    @Override
    public boolean getRendersChildren() {
        CustomUITestCase.doLookupTest();
        return super.getRendersChildren();
    }

    @Override
    public List<UIComponent> getChildren() {
        CustomUITestCase.doLookupTest();
        return super.getChildren();
    }

    @Override
    public int getChildCount() {
        CustomUITestCase.doLookupTest();
        return super.getChildCount();
    }

    @Override
    public UIComponent findComponent(String expr) {
        CustomUITestCase.doLookupTest();
        return super.findComponent(expr);
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback) throws FacesException {
        CustomUITestCase.doLookupTest();
        return super.invokeOnComponent(context, clientId, callback);
    }

    @Override
    public Map<String, UIComponent> getFacets() {
        CustomUITestCase.doLookupTest();
        return super.getFacets();
    }

    @Override
    public int getFacetCount() {
        CustomUITestCase.doLookupTest();
        return super.getFacetCount();
    }

    @Override
    public UIComponent getFacet(String name) {
        CustomUITestCase.doLookupTest();
        return super.getFacet(name);
    }

    @Override
    public Iterator<UIComponent> getFacetsAndChildren() {
        CustomUITestCase.doLookupTest();
        return super.getFacetsAndChildren();
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        CustomUITestCase.doLookupTest();
        super.broadcast(event);
    }

    @Override
    public void decode(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.decode(context);
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        CustomUITestCase.doLookupTest();
        super.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        CustomUITestCase.doLookupTest();
        super.encodeEnd(context);
    }

    @Override
    protected void addFacesListener(FacesListener listener) {
        CustomUITestCase.doLookupTest();
        super.addFacesListener(listener);
    }

    @Override
    protected FacesListener[] getFacesListeners(Class clazz) {
        CustomUITestCase.doLookupTest();
        return super.getFacesListeners(clazz);
    }

    @Override
    protected void removeFacesListener(FacesListener listener) {
        CustomUITestCase.doLookupTest();
        super.removeFacesListener(listener);
    }

    @Override
    public void queueEvent(FacesEvent event) {
        CustomUITestCase.doLookupTest();
        super.queueEvent(event);
    }

    @Override
    public void processDecodes(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.processDecodes(context);
    }

    @Override
    public void processValidators(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.processValidators(context);
    }

    @Override
    public void processUpdates(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.processUpdates(context);
    }

    @Override
    public Object processSaveState(FacesContext context) {
        CustomUITestCase.doLookupTest();
        return super.processSaveState(context);
    }

    @Override
    public void processRestoreState(FacesContext context, Object state) {
        CustomUITestCase.doLookupTest();
        super.processRestoreState(context, state);
    }

    @Override
    protected FacesContext getFacesContext() {
        CustomUITestCase.doLookupTest();
        return super.getFacesContext();
    }

    @Override
    protected Renderer getRenderer(FacesContext context) {
        CustomUITestCase.doLookupTest();
        return super.getRenderer(context);
    }

    @Override
    public Object saveState(FacesContext context) {
        CustomUITestCase.doLookupTest();
        return super.saveState(context);
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        CustomUITestCase.doLookupTest();
        super.restoreState(context, state);
    }

    @Override
    public boolean isTransient() {
        CustomUITestCase.doLookupTest();
        return super.isTransient();
    }

    @Override
    public void setTransient(boolean transientFlag) {
        CustomUITestCase.doLookupTest();
        super.setTransient(transientFlag);
    }
    
    @Override
    public ValueExpression getValueExpression(String name) {
        CustomUITestCase.doLookupTest();
        return super.getValueExpression(name);
    }

    @Override
    public void setValueExpression(String name, ValueExpression binding) {
        CustomUITestCase.doLookupTest();
        super.setValueExpression(name, binding);
    }

    @Override
    public String getContainerClientId(FacesContext context) {
        CustomUITestCase.doLookupTest();
        return super.getContainerClientId(context);
    }

    @Override
    public void encodeAll(FacesContext context) throws IOException {
        CustomUITestCase.doLookupTest();
        super.encodeAll(context);
    }

    @Override
    public Object getSubmittedValue() {
        CustomUITestCase.doLookupTest();
        return super.getSubmittedValue();
    }

    @Override
    public void setSubmittedValue(Object submittedValue) {
        CustomUITestCase.doLookupTest();
        super.setSubmittedValue(submittedValue);
    }

    @Override
    public void setValue(Object value) {
        CustomUITestCase.doLookupTest();
        super.setValue(value);
    }

    @Override
    public void resetValue() {
        CustomUITestCase.doLookupTest();
        super.resetValue();
    }

    @Override
    public boolean isLocalValueSet() {
        CustomUITestCase.doLookupTest();
        return super.isLocalValueSet();
    }

    @Override
    public void setLocalValueSet(boolean localValueSet) {
        CustomUITestCase.doLookupTest();
        super.setLocalValueSet(localValueSet);
    }

    @Override
    public boolean isRequired() {
        CustomUITestCase.doLookupTest();
        return super.isRequired();
    }

    @Override
    public String getRequiredMessage() {
        CustomUITestCase.doLookupTest();
        return super.getRequiredMessage();
    }

    @Override
    public void setRequiredMessage(String message) {
        CustomUITestCase.doLookupTest();
        super.setRequiredMessage(message);
    }

    @Override
    public String getConverterMessage() {
        CustomUITestCase.doLookupTest();
        return super.getConverterMessage();
    }

    @Override
    public void setConverterMessage(String message) {
        CustomUITestCase.doLookupTest();
        super.setConverterMessage(message);
    }

    @Override
    public String getValidatorMessage() {
        CustomUITestCase.doLookupTest();
        return super.getValidatorMessage();
    }

    @Override
    public void setValidatorMessage(String message) {
        CustomUITestCase.doLookupTest();
        super.setValidatorMessage(message);
    }

    @Override
    public boolean isValid() {
        CustomUITestCase.doLookupTest();
        return super.isValid();
    }

    @Override
    public void setValid(boolean valid) {
        CustomUITestCase.doLookupTest();
        super.setValid(valid);
    }

    @Override
    public void setRequired(boolean required) {
        CustomUITestCase.doLookupTest();
        super.setRequired(required);
    }

    @Override
    public boolean isImmediate() {
        CustomUITestCase.doLookupTest();
        return super.isImmediate();
    }

    @Override
    public void setImmediate(boolean immediate) {
        CustomUITestCase.doLookupTest();
        super.setImmediate(immediate);
    }

    @Override
    public MethodBinding getValidator() {
        CustomUITestCase.doLookupTest();
        return super.getValidator();
    }

    @Override
    public void setValidator(MethodBinding validatorBinding) {
        CustomUITestCase.doLookupTest();
        super.setValidator(validatorBinding);
    }

    @Override
    public MethodBinding getValueChangeListener() {
        CustomUITestCase.doLookupTest();
        return super.getValueChangeListener();
    }

    @Override
    public void setValueChangeListener(MethodBinding valueChangeListener) {
        CustomUITestCase.doLookupTest();
        super.setValueChangeListener(valueChangeListener);
    }

    @Override
    public void updateModel(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.updateModel(context);
    }

    @Override
    public void validate(FacesContext context) {
        CustomUITestCase.doLookupTest();
        super.validate(context);
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object newSubmittedValue) throws ConverterException {
        CustomUITestCase.doLookupTest();
        return super.getConvertedValue(context, newSubmittedValue);
    }

    @Override
    protected void validateValue(FacesContext context, Object newValue) {
        CustomUITestCase.doLookupTest();
        super.validateValue(context, newValue);
    }

    @Override
    protected boolean compareValues(Object previous, Object value) {
        CustomUITestCase.doLookupTest();
        return super.compareValues(previous, value);
    }

    @Override
    public void addValidator(Validator validator) {
        CustomUITestCase.doLookupTest();
        super.addValidator(validator);
    }

    @Override
    public Validator[] getValidators() {
        CustomUITestCase.doLookupTest();
        return super.getValidators();
    }

    @Override
    public void removeValidator(Validator validator) {
        CustomUITestCase.doLookupTest();
        super.removeValidator(validator);
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        CustomUITestCase.doLookupTest();
        super.addValueChangeListener(listener);
    }

    @Override
    public ValueChangeListener[] getValueChangeListeners() {
        CustomUITestCase.doLookupTest();
        return super.getValueChangeListeners();
    }

    @Override
    public void removeValueChangeListener(ValueChangeListener listener) {
        CustomUITestCase.doLookupTest();
        super.removeValueChangeListener(listener);
    }

    @Override
    public Converter getConverter() {
        CustomUITestCase.doLookupTest();
        return super.getConverter();
    }

    @Override
    public void setConverter(Converter converter) {
        CustomUITestCase.doLookupTest();
        super.setConverter(converter);
    }

    @Override
    public Object getLocalValue() {
        CustomUITestCase.doLookupTest();
        return super.getLocalValue();
    }

    @Override
    public Object getValue() {
        CustomUITestCase.doLookupTest();
        return super.getValue();
    }    
    
}