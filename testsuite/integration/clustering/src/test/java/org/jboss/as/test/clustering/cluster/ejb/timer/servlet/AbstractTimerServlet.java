/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.timer.servlet;

import java.io.IOException;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.LocalEJBDirectory;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractTimerServlet extends HttpServlet {
    private static final long serialVersionUID = -5679222023732143513L;
    protected static final String MODULE = "module";

    private final Set<Class<? extends ManualTimerBean>> manualTimerClasses;
    private final Set<Class<? extends TimerBean>> autoTimerClasses;

    protected AbstractTimerServlet(Set<Class<? extends ManualTimerBean>> manualTimerClasses, Set<Class<? extends TimerBean>> autoTimerClasses) {
        this.manualTimerClasses = manualTimerClasses;
        this.autoTimerClasses = autoTimerClasses;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getServletContext().log(String.format("%s: http://%s:%s%s?%s", request.getMethod(), request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getQueryString()));
        super.service(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String module = request.getParameter(MODULE);
        try (EJBDirectory directory = new LocalEJBDirectory(module)) {
            for (Class<? extends ManualTimerBean> beanClass : this.manualTimerClasses) {
                ManualTimerBean bean = directory.lookupSingleton(beanClass, ManualTimerBean.class);
                bean.cancel();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String module = request.getParameter(MODULE);
        try (EJBDirectory directory = new LocalEJBDirectory(module)) {
            Map<Class<? extends TimerBean>, TimerBean> beans = new IdentityHashMap<>();
            for (Class<? extends ManualTimerBean> beanClass : this.manualTimerClasses) {
                beans.put(beanClass, directory.lookupSingleton(beanClass, ManualTimerBean.class));
            }
            for (Class<? extends TimerBean> beanClass : this.autoTimerClasses) {
                beans.put(beanClass, directory.lookupSingleton(beanClass, TimerBean.class));
            }
            for (Map.Entry<Class<? extends TimerBean>, TimerBean> entry : beans.entrySet()) {
                for (Instant timeout : entry.getValue().getTimeouts()) {
                    response.addDateHeader(entry.getKey().getName(), timeout.toEpochMilli());
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String module = request.getParameter(MODULE);
        try (EJBDirectory directory = new LocalEJBDirectory(module)) {
            Map<Class<? extends TimerBean>, TimerBean> beans = new IdentityHashMap<>();
            for (Class<? extends ManualTimerBean> beanClass : this.manualTimerClasses) {
                beans.put(beanClass, directory.lookupSingleton(beanClass, ManualTimerBean.class));
            }
            for (Class<? extends TimerBean> beanClass : this.autoTimerClasses) {
                beans.put(beanClass, directory.lookupSingleton(beanClass, TimerBean.class));
            }
            for (Map.Entry<Class<? extends TimerBean>, TimerBean> entry : beans.entrySet()) {
                response.addIntHeader(entry.getKey().getName(), entry.getValue().getTimers());
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String module = request.getParameter(MODULE);
        try (EJBDirectory directory = new LocalEJBDirectory(module)) {
            for (Class<? extends ManualTimerBean> beanClass : this.manualTimerClasses) {
                directory.lookupSingleton(beanClass, ManualTimerBean.class).createTimer();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
