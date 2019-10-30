/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

        // register compositeELResolver with JSP
        jspAppContext.addELResolver(beanManager.getELResolver());
        jspAppContext.addELContextListener(Reflections.<ELContextListener>newInstance("org.jboss.weld.module.web.el.WeldELContextListener", getClass().getClassLoader()));

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
