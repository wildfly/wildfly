/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.timerservice.AutoTimer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.ScheduleMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.ejb.spec.TimerMetaData;
import org.jboss.modules.Module;

import javax.ejb.ScheduleExpression;

/**
 * Deployment unit processor that merges the annotation information with the information in the deployment descriptor
 *
 * @author Stuart Douglas
 */
public class TimeoutMethodDeploymentDescriptorProcessor extends AbstractEjbXmlDescriptorProcessor<SessionBeanMetaData> {

    @Override
    protected Class<SessionBeanMetaData> getMetaDataType() {
        return SessionBeanMetaData.class;
    }

    @Override
    protected void processBeanMetaData(final SessionBeanMetaData beanMetaData, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final SessionBeanComponentDescription sessionBean = (SessionBeanComponentDescription) moduleDescription.getComponentByName(beanMetaData.getEjbName());
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }
        if (sessionBean == null) {
            //should not happen
            return;
        }

        final Class<?> componentClass;
        try {
            componentClass = module.getClassLoader().loadClass(sessionBean.getComponentClassName());
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Could not load EJB class " + sessionBean.getComponentClassName());
        }
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        if (beanMetaData.getTimeoutMethod() != null) {
            parseTimeoutMethod(beanMetaData, sessionBean, componentClass, deploymentReflectionIndex);
        }
        parseScheduleMethods(beanMetaData, sessionBean, componentClass, deploymentReflectionIndex);
    }

    private void parseScheduleMethods(final SessionBeanMetaData beanMetaData, final SessionBeanComponentDescription sessionBean, final Class<?> componentClass, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        if (beanMetaData instanceof SessionBean31MetaData) {
            SessionBean31MetaData md = (SessionBean31MetaData) beanMetaData;
            if (md.getTimers() != null) {
                for (final TimerMetaData timer : md.getTimers()) {
                    AutoTimer autoTimer = new AutoTimer();

                    autoTimer.getTimerConfig().setInfo(timer.getInfo());
                    autoTimer.getTimerConfig().setPersistent(timer.isPersistent());

                    final ScheduleExpression scheduleExpression = autoTimer.getScheduleExpression();
                    final ScheduleMetaData schedule = timer.getSchedule();
                    if (schedule != null) {
                        scheduleExpression.dayOfMonth(schedule.getDayOfMonth());
                        scheduleExpression.dayOfWeek(schedule.getDayOfWeek());
                        scheduleExpression.hour(schedule.getHour());
                        scheduleExpression.minute(schedule.getMinute());
                        scheduleExpression.month(schedule.getMonth());
                        scheduleExpression.second(schedule.getSecond());
                        scheduleExpression.year(schedule.getYear());
                    }
                    if (timer.getEnd() != null) {
                        scheduleExpression.end(timer.getEnd().getTime());
                    }
                    if (timer.getStart() != null) {
                        scheduleExpression.start(timer.getStart().getTime());
                    }
                    scheduleExpression.timezone(timer.getTimezone());
                    sessionBean.addScheduleMethod(MethodResolutionUtils.resolveMethod(timer.getTimeoutMethod(), componentClass, deploymentReflectionIndex), autoTimer);

                }
            }

        }
    }

    private void parseTimeoutMethod(final SessionBeanMetaData beanMetaData, final SessionBeanComponentDescription sessionBean, final Class<?> componentClass, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        //resolve timeout methods
        final NamedMethodMetaData methodData = beanMetaData.getTimeoutMethod();
        sessionBean.setTimeoutMethod(MethodResolutionUtils.resolveMethod(methodData, componentClass, deploymentReflectionIndex));
    }
}
