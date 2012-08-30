/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.webtier.jsp;

import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.weld.util.Reflections;

/**
 * The Web Beans JSP initialization listener
 *
 * @author Pete Muir
 * @author Stuart Douglas
 */
public class JspInitializationListener implements ExpressionFactoryWrapper{

    public static final JspInitializationListener INSTANCE = new JspInitializationListener();

    @Override
    public ExpressionFactory wrap(ExpressionFactory expressionFactory, ServletContext servletContext) {
        BeanManager beanManager = getBeanManager();
        if(beanManager == null) {
            //this should never happen
            return expressionFactory;
        }
        // get JspApplicationContext.
        JspApplicationContext jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext( servletContext);

        // register compositeELResolver with JSP
        jspAppContext.addELResolver(beanManager.getELResolver());
        jspAppContext.addELContextListener(Reflections.<ELContextListener>newInstance("org.jboss.weld.el.WeldELContextListener", getClass().getClassLoader()));

        return beanManager.wrapExpressionFactory(expressionFactory);
    }

    private BeanManager getBeanManager() {
        try {
            return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            return null;
        }
    }

}
