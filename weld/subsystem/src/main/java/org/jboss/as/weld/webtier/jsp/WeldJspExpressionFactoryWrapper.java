/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.webtier.jsp;

import jakarta.el.ELContextListener;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspFactory;

import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.weld.util.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The Web Beans Jakarta Server Pages initialization listener
 *
 * @author Pete Muir
 * @author Stuart Douglas
 */
public class WeldJspExpressionFactoryWrapper implements ExpressionFactoryWrapper {

    public static final WeldJspExpressionFactoryWrapper INSTANCE = new WeldJspExpressionFactoryWrapper();

    @Override
    public ExpressionFactory wrap(ExpressionFactory expressionFactory, ServletContext servletContext) {
        BeanManager beanManager = getBeanManager();
        if(beanManager == null) {
            //this should never happen
            return expressionFactory;
        }
        // get JspApplicationContext.
        JspApplicationContext jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext( servletContext);

        // register compositeELResolver with Jakarta Server Pages
        try {
            // Test for CDI 4.1+, if present use the new ElAwareBeanManager
            // This block can be replaced by non-reflective variant soon as WFLY runs CDI 4.1+
            Class<?> elAwareBmClass = Class.forName("jakarta.enterprise.inject.spi.el.ELAwareBeanManager");
            Method getELResolver = elAwareBmClass.getMethod("getELResolver");
            Object elAwareBm = elAwareBmClass.cast(beanManager);
            jspAppContext.addELResolver((ELResolver) getELResolver.invoke(elAwareBm));
            jspAppContext.addELContextListener(Reflections.<ELContextListener>newInstance("org.jboss.weld.module.web.el.WeldELContextListener", getClass().getClassLoader()));

            Method wrapExpressionFactory = elAwareBmClass.getMethod("wrapExpressionFactory", ExpressionFactory.class);
            return (ExpressionFactory) wrapExpressionFactory.invoke(elAwareBmClass.cast(elAwareBm), expressionFactory);
        } catch (ClassNotFoundException e) {
            // this is CDI 4.0, use the standard BM
            jspAppContext.addELResolver(beanManager.getELResolver());
            jspAppContext.addELContextListener(Reflections.<ELContextListener>newInstance("org.jboss.weld.module.web.el.WeldELContextListener", getClass().getClassLoader()));
            return beanManager.wrapExpressionFactory(expressionFactory);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private BeanManager getBeanManager() {
        try {
            return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            return null;
        }
    }

}
