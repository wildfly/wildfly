/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.mail.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.MailSessionMetaData;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;

/**
 * Deployment processor responsible for processing {@link MailSessionDefinition} and {@link MailSessionDefinitions}
 * and creating {@link org.jboss.as.ee.component.BindingConfiguration}s out of them
 *
 * @author Tomaz Cerar
 */
public class MailSessionDefinitionAnnotationParser implements DeploymentUnitProcessor {

    private static final DotName MAIL_SESSION_DEFINITION = DotName.createSimple(MailSessionDefinition.class.getName());
    private static final DotName MAIL_SESSION_DEFINITIONS = DotName.createSimple(MailSessionDefinitions.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);

        // @MailSessionDefinitions
        List<AnnotationInstance> mailSessionDefinitions = index.getAnnotations(MAIL_SESSION_DEFINITIONS);
        if (mailSessionDefinitions != null) {
            for (AnnotationInstance annotation : mailSessionDefinitions) {
                final AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@MailSessionDefinitions", target);
                }
                // get the nested @MailSessionDefinition out of the outer @MailSessionDefinitions
                List<AnnotationInstance> mailSessions = this.getNestedMailSessionAnnotations(annotation);
                // process the nested @MailSessionDefinition
                for (AnnotationInstance mailSession : mailSessions) {
                    // create binding configurations out of it
                    this.processMailSession(eeModuleDescription, mailSession, (ClassInfo) target);
                }
            }
        }

        // @MailSessionDefinition
        List<AnnotationInstance> mailSessions = index.getAnnotations(MAIL_SESSION_DEFINITION);
        if (mailSessions != null) {
            for (AnnotationInstance mailSession : mailSessions) {
                final AnnotationTarget target = mailSession.target();
                if (!(target instanceof ClassInfo)) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@MailSessionDefinition", target);
                }
                // create binding configurations out of it
                this.processMailSession(eeModuleDescription, mailSession, (ClassInfo) target);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processMailSession(final EEModuleDescription eeModuleDescription, final AnnotationInstance mailSessionDefinition, final ClassInfo targetClass) throws DeploymentUnitProcessingException {
        // create BindingConfiguration out of the @MailSessionDefinition annotation
        final BindingConfiguration bindingConfiguration = this.getBindingConfiguration(mailSessionDefinition);
        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(targetClass.name().toString());
        // add the binding configuration via a class configurator
        classDescription.getBindingConfigurations().add(bindingConfiguration);
    }

    private BindingConfiguration getBindingConfiguration(final AnnotationInstance annotationInstance) {

        final AnnotationValue nameValue = annotationInstance.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@MailSessionDefinition", "name");
        }

        /*String storeProtocol() default "";
        String transportProtocol() default "";
        String host() default "";
        String user() default "";
        String password() default "";
        String from() default "";*/

        MailSessionMetaData metaData = new MailSessionMetaData();
        metaData.setTransportProtocol(asString(annotationInstance, "transportProtocol"));
        metaData.setStoreProtocol(asString(annotationInstance, "storeProtocol"));
        metaData.setHost(asString(annotationInstance, "host"));
        metaData.setUser(asString(annotationInstance, "user"));
        metaData.setPassword(asString(annotationInstance, "password"));
        metaData.setFrom(asString(annotationInstance, "from"));
        metaData.setName(nameValue.asString());
        String jndiName = MailSessionAdd.getJndiName(nameValue.asString());
        PropertiesMetaData properties = new PropertiesMetaData();

        for (String fullProp : asArray(annotationInstance, "properties")) {
            PropertyMetaData p = new PropertyMetaData();
            String[] prop = fullProp.split("=", 2);
            p.setName(prop[0]);
            p.setValue(prop[1]);
            properties.add(p);
        }
        metaData.setProperties(properties);


        SessionProvider provider = SessionProviderFactory.create(metaData);
        final DirectMailSessionInjectionSource source = new DirectMailSessionInjectionSource(jndiName, provider);
        return new BindingConfiguration(jndiName, source);
    }

    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asString().isEmpty() ? null : value.asString();
    }

    private String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? new String[0] : value.asStringArray();
    }

    /**
     * Returns the nested {@link MailSessionDefinition} annotations out of the outer {@link MailSessionDefinitions} annotation
     *
     * @param mailSessionDefinitions The outer {@link MailSessionDefinitions} annotation
     * @return annotations
     */
    private List<AnnotationInstance> getNestedMailSessionAnnotations(AnnotationInstance mailSessionDefinitions) {
        if (mailSessionDefinitions == null) {
            return Collections.emptyList();
        }
        AnnotationInstance[] nested = mailSessionDefinitions.value().asNestedArray();
        return Arrays.asList(nested);
    }

}
