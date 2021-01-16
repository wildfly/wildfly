/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jsf.managedbean.gc;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.jboss.logging.Logger;

@ManagedBean
@ViewScoped
public class CountBeanViewScoped implements Serializable {

    private static final long serialVersionUID = 1L;

    @ManagedProperty("#{initBeanViewScoped}")
    private InitBeanViewScoped initBeanViewScoped;

    private static final Logger LOG = Logger.getLogger(CountBeanViewScoped.class.getName());

    private int count;

    @PostConstruct
    public void init() {
        LOG.debug("CountBeanViewScoped#Initializing counter with @PostConstruct ...");
        count = initBeanViewScoped.getInit();
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Destroyed View Scoped CountBean");
        TestResultsBean.setPreDestroyViewScoped(true);
    }

    public String invalidateGC() {

        LOG.debug("Running View Scoped Garbage Collect, invalidate session so view will be destroyed");
        System.gc();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.invalidateSession();

        return "";
    }

    public void setInitBeanViewScoped(InitBeanViewScoped initBean) {
        this.initBeanViewScoped = initBean;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
