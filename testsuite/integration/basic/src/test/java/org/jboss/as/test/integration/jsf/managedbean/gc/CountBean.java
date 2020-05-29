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
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.jboss.logging.Logger;

@ManagedBean
@SessionScoped
public class CountBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @ManagedProperty("#{initBean}")
    private InitBean initBean;

    private static final Logger LOG = Logger.getLogger(CountBean.class.getName());

    private int count;

    @PostConstruct
    public void init() {
        LOG.debug("CountBean#Initializing counter with @PostConstruct ...");
        count = initBean.getInit();
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Destroyed CountBean");
        TestResultsBean.setPreDestroySessionScoped(true);
    }

    public String invalidateGC() {

        LOG.debug("Running Garbage Collect and Invalidating Session");

        System.gc();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.invalidateSession();

        return "";
    }

    public void setInitBean(InitBean initBean) {
        this.initBean = initBean;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
