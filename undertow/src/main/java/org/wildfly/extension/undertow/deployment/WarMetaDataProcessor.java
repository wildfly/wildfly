/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.merge.javaee.spec.SecurityRolesMetaDataMerger;
import org.jboss.metadata.merge.web.jboss.JBossWebMetaDataMerger;
import org.jboss.metadata.merge.web.spec.WebCommonMetaDataMerger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AbsoluteOrderingMetaData;
import org.jboss.metadata.web.spec.OrderingElementMetaData;
import org.jboss.metadata.web.spec.Web25MetaData;
import org.jboss.metadata.web.spec.Web30MetaData;
import org.jboss.metadata.web.spec.Web31MetaData;
import org.jboss.metadata.web.spec.WebCommonMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * Merge all metadata into a main JBossWebMetaData.
 *
 * @author Remy Maucherat
 * @author Thomas.Diesler@jboss.com
 */
public class WarMetaDataProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;

        boolean isComplete = false;
        WebMetaData specMetaData = warMetaData.getWebMetaData();
        if (specMetaData != null) {
            if (specMetaData instanceof Web25MetaData) {
                isComplete |= ((Web25MetaData) specMetaData).isMetadataComplete();
            } else if (specMetaData instanceof Web30MetaData) {
                isComplete |= ((Web30MetaData) specMetaData).isMetadataComplete();
            } else {
                // As per Servlet 3.0 spec, metadata is not completed unless it's set to true in web.xml.
                // Hence, any web.xml 2.4 or earlier deployment is not metadata completed.
                isComplete = false;
            }
        }

        // Find all fragments that have been processed by deployers, and place
        // them in a map keyed by location
        LinkedList<String> order = new LinkedList<String>();
        List<WebOrdering> orderings = new ArrayList<WebOrdering>();
        HashSet<String> jarsSet = new HashSet<String>();
        Set<VirtualFile> overlays = new HashSet<VirtualFile>();
        Map<String, VirtualFile> scis = new HashMap<String, VirtualFile>();
        boolean fragmentFound = false;
        Map<String, WebFragmentMetaData> webFragments = warMetaData.getWebFragmentsMetaData();
        List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : resourceRoots) {
            if (resourceRoot.getRoot().getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                jarsSet.add(resourceRoot.getRootName());
                // Find overlays
                VirtualFile overlay = resourceRoot.getRoot().getChild("META-INF/resources");
                if (overlay.exists()) {
                    overlays.add(overlay);
                }
            }


            //we load SCI's directly from the war not just from jars
            //not required by spec but other containers do it
            //see WFLY-9081
            // Find ServletContainerInitializer services
            VirtualFile sci = resourceRoot.getRoot().getChild("META-INF/services/javax.servlet.ServletContainerInitializer");
            if (sci.exists()) {
                scis.put(resourceRoot.getRootName(), sci);
            }
        }

        if (!isComplete) {
            HashSet<String> jarsWithoutFragmentsSet = new HashSet<String>();
            jarsWithoutFragmentsSet.addAll(jarsSet);
            for (String jarName : webFragments.keySet()) {
                fragmentFound = true;
                WebFragmentMetaData fragmentMetaData = webFragments.get(jarName);
                webFragments.put(jarName, fragmentMetaData);
                WebOrdering webOrdering = new WebOrdering();
                webOrdering.setName(fragmentMetaData.getName());
                webOrdering.setJar(jarName);
                jarsWithoutFragmentsSet.remove(jarName);
                if (fragmentMetaData.getOrdering() != null) {
                    if (fragmentMetaData.getOrdering().getAfter() != null) {
                        for (OrderingElementMetaData orderingElementMetaData : fragmentMetaData.getOrdering().getAfter()
                                .getOrdering()) {
                            if (orderingElementMetaData.isOthers()) {
                                webOrdering.setAfterOthers(true);
                            } else {
                                webOrdering.addAfter(orderingElementMetaData.getName());
                            }
                        }
                    }
                    if (fragmentMetaData.getOrdering().getBefore() != null) {
                        for (OrderingElementMetaData orderingElementMetaData : fragmentMetaData.getOrdering().getBefore()
                                .getOrdering()) {
                            if (orderingElementMetaData.isOthers()) {
                                webOrdering.setBeforeOthers(true);
                            } else {
                                webOrdering.addBefore(orderingElementMetaData.getName());
                            }
                        }
                    }
                }
                orderings.add(webOrdering);
            }
            // If there is no fragment, still consider it for ordering as a
            // fragment specifying no name and no order
            for (String jarName : jarsWithoutFragmentsSet) {
                WebOrdering ordering = new WebOrdering();
                ordering.setJar(jarName);
                orderings.add(ordering);
            }

        }

        if (!fragmentFound) {
            // Drop the order as there is no fragment in the webapp
            orderings.clear();
        }

        // Generate web fragments parsing order
        AbsoluteOrderingMetaData absoluteOrderingMetaData = null;
        if (!isComplete && specMetaData instanceof Web30MetaData) {
            absoluteOrderingMetaData = ((Web30MetaData) specMetaData).getAbsoluteOrdering();
        }
        if (absoluteOrderingMetaData != null) {
            // Absolute ordering from web.xml, any relative fragment ordering is ignored
            int otherPos = -1;
            int i = 0;
            for (OrderingElementMetaData orderingElementMetaData : absoluteOrderingMetaData.getOrdering()) {
                if (orderingElementMetaData.isOthers()) {
                    if (otherPos >= 0) {
                        throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidMultipleOthers());
                    }
                    otherPos = i;
                } else {
                    boolean found = false;
                    for (WebOrdering ordering : orderings) {
                        if (orderingElementMetaData.getName().equals(ordering.getName())) {
                            order.add(ordering.getJar());
                            jarsSet.remove(ordering.getJar());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        UndertowLogger.ROOT_LOGGER.invalidAbsoluteOrdering(orderingElementMetaData.getName());
                    } else {
                        i++;
                    }
                }
            }
            if (otherPos >= 0) {
                order.addAll(otherPos, jarsSet);
                jarsSet.clear();
            }
        } else if (orderings.size() > 0) {
            // Resolve relative ordering
            try {
                resolveOrder(orderings, order);
            } catch (IllegalStateException e) {
                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrdering(), e);
            }
            jarsSet.clear();
        } else {
            // No order specified
            order.addAll(jarsSet);
            jarsSet.clear();
            warMetaData.setNoOrder(true);
        }

        if (UndertowLogger.ROOT_LOGGER.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            builder.append("Resolved order: [ ");
            for (String jar : order) {
                builder.append(jar).append(' ');
            }
            builder.append(']');
            UndertowLogger.ROOT_LOGGER.debug(builder.toString());
        }

        warMetaData.setOrder(order);
        warMetaData.setOverlays(overlays);
        warMetaData.setScis(scis);

        Map<String, WebMetaData> annotationsMetaData = warMetaData.getAnnotationsMetaData();

        // The fragments and corresponding annotations will need to be merged in order
        // For each JAR in the order:
        // - Merge the annotation metadata into the fragment meta data (unless the fragment exists and is meta data complete)
        // - Merge the fragment metadata into merged fragment meta data
        WebCommonMetaData mergedFragmentMetaData = new WebCommonMetaData();
        if (specMetaData == null) {
            // If there is no web.xml, it has to be considered to be the latest version
            specMetaData = new Web31MetaData();
            specMetaData.setVersion("3.1");
        }
        // Augment with meta data from annotations in /WEB-INF/classes
        WebMetaData annotatedMetaData = annotationsMetaData.get("classes");
        if (annotatedMetaData == null && deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST)) {
            annotatedMetaData = annotationsMetaData.get(deploymentUnit.getName());
        }
        if (annotatedMetaData != null) {
            if (isComplete) {
                // Discard @WebFilter, @WebListener and @WebServlet
                annotatedMetaData.setFilters(null);
                annotatedMetaData.setFilterMappings(null);
                annotatedMetaData.setListeners(null);
                annotatedMetaData.setServlets(null);
                annotatedMetaData.setServletMappings(null);
            }
            WebCommonMetaDataMerger.augment(specMetaData, annotatedMetaData, null, true);
        }
        // Augment with meta data from fragments and annotations from the corresponding JAR
        for (String jar : order) {
            WebFragmentMetaData webFragmentMetaData = webFragments.get(jar);
            if (webFragmentMetaData == null || isComplete) {
                webFragmentMetaData = new WebFragmentMetaData();
                // Add non overriding default distributable flag
                webFragmentMetaData.setDistributable(new EmptyMetaData());
            }
            WebMetaData jarAnnotatedMetaData = annotationsMetaData.get(jar);
            if ((isComplete || webFragmentMetaData.isMetadataComplete()) && jarAnnotatedMetaData != null) {
                // Discard @WebFilter, @WebListener and @WebServlet
                jarAnnotatedMetaData.setFilters(null);
                jarAnnotatedMetaData.setFilterMappings(null);
                jarAnnotatedMetaData.setListeners(null);
                jarAnnotatedMetaData.setServlets(null);
                jarAnnotatedMetaData.setServletMappings(null);
            }
            if (jarAnnotatedMetaData != null) {
                // Merge annotations corresponding to the JAR
                WebCommonMetaDataMerger.augment(webFragmentMetaData, jarAnnotatedMetaData, null, true);
            }
            // Merge fragment meta data according to the conflict rules
            try {
                WebCommonMetaDataMerger.augment(mergedFragmentMetaData, webFragmentMetaData, specMetaData, false);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebFragment(jar), e);
            }
        }
        // Augment with meta data from annotations from JARs excluded from the order
        for (String jar : jarsSet) {
            WebFragmentMetaData webFragmentMetaData = new WebFragmentMetaData();
            // Add non overriding default distributable flag
            webFragmentMetaData.setDistributable(new EmptyMetaData());
            WebMetaData jarAnnotatedMetaData = annotationsMetaData.get(jar);
            if (jarAnnotatedMetaData != null) {
                // Discard @WebFilter, @WebListener and @WebServlet
                jarAnnotatedMetaData.setFilters(null);
                jarAnnotatedMetaData.setFilterMappings(null);
                jarAnnotatedMetaData.setListeners(null);
                jarAnnotatedMetaData.setServlets(null);
                jarAnnotatedMetaData.setServletMappings(null);
            }
            if (jarAnnotatedMetaData != null) {
                // Merge annotations corresponding to the JAR
                WebCommonMetaDataMerger.augment(webFragmentMetaData, jarAnnotatedMetaData, null, true);
            }
            // Merge fragment meta data according to the conflict rules
            try {
                WebCommonMetaDataMerger.augment(mergedFragmentMetaData, webFragmentMetaData, specMetaData, false);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebFragment(jar), e);
            }
        }

        WebCommonMetaDataMerger.augment(specMetaData, mergedFragmentMetaData, null, true);

        List<WebMetaData> additional = warMetaData.getAdditionalModuleAnnotationsMetadata();
        if (additional != null && !isComplete) {
            //augument with annotations from additional modules
            for (WebMetaData annotations : additional) {
                // Merge annotations corresponding to the JAR
                WebCommonMetaDataMerger.augment(specMetaData, annotations, null, true);
            }
        }

        // Override with meta data (JBossWebMetaData) Create a merged view
        JBossWebMetaData mergedMetaData = new JBossWebMetaData();
        JBossWebMetaData metaData = warMetaData.getJBossWebMetaData();
        JBossWebMetaDataMerger.merge(mergedMetaData, metaData, specMetaData);
        // FIXME: Incorporate any ear level overrides

        // Use the OSGi Web-ContextPath if not given otherwise
        String contextRoot = mergedMetaData.getContextRoot();
        Manifest manifest = deploymentUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (contextRoot == null && manifest != null) {
            contextRoot = manifest.getMainAttributes().getValue("Web-ContextPath");
            mergedMetaData.setContextRoot(contextRoot);
        }
        warMetaData.setMergedJBossWebMetaData(mergedMetaData);

        if (mergedMetaData.isMetadataComplete()) {
            MetadataCompleteMarker.setMetadataComplete(deploymentUnit, true);
        }

        //now attach any JNDI binding related information to the deployment
        if (mergedMetaData.getJndiEnvironmentRefsGroup() != null) {
            final DeploymentDescriptorEnvironment bindings = new DeploymentDescriptorEnvironment("java:module/env/", mergedMetaData.getJndiEnvironmentRefsGroup());
            deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, bindings);
        }

        //override module name if applicable
        if (mergedMetaData.getModuleName() != null && !mergedMetaData.getModuleName().isEmpty()) {
            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            description.setModuleName(mergedMetaData.getModuleName());
        }

        //WFLY-3102 EJB in WAR should inherit WAR's security domain
        if(mergedMetaData.getSecurityDomain() != null) {
            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            description.setDefaultSecurityDomain(mergedMetaData.getSecurityDomain());
        }

        //merge security roles from the ear
        DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            final EarMetaData earMetaData = parent.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (earMetaData != null) {
                SecurityRolesMetaData earSecurityRolesMetaData = earMetaData.getSecurityRoles();
                if(earSecurityRolesMetaData != null) {
                    if(mergedMetaData.getSecurityRoles() == null) {
                        mergedMetaData.setSecurityRoles(new SecurityRolesMetaData());
                    }
                    SecurityRolesMetaDataMerger.merge(mergedMetaData.getSecurityRoles(), mergedMetaData.getSecurityRoles(), earSecurityRolesMetaData);
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    /**
     * Utility class to associate the logical name with the JAR name, needed
     * during the order resolving.
     */
    protected static class WebOrdering implements Serializable {

        private static final long serialVersionUID = 5603203103871892211L;

        protected String jar = null;
        protected String name = null;
        protected List<String> after = new ArrayList<String>();
        protected List<String> before = new ArrayList<String>();
        protected boolean afterOthers = false;
        protected boolean beforeOthers = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAfter() {
            return after;
        }

        public void addAfter(String name) {
            after.add(name);
        }

        public List<String> getBefore() {
            return before;
        }

        public void addBefore(String name) {
            before.add(name);
        }

        public String getJar() {
            return jar;
        }

        public void setJar(String jar) {
            this.jar = jar;
        }

        public boolean isAfterOthers() {
            return afterOthers;
        }

        public void setAfterOthers(boolean afterOthers) {
            this.afterOthers = afterOthers;
        }

        public boolean isBeforeOthers() {
            return beforeOthers;
        }

        public void setBeforeOthers(boolean beforeOthers) {
            this.beforeOthers = beforeOthers;
        }

    }

    protected static class Ordering {
        protected WebOrdering ordering;
        protected Set<Ordering> after = new HashSet<Ordering>();
        protected Set<Ordering> before = new HashSet<Ordering>();
        protected boolean afterOthers = false;
        protected boolean beforeOthers = false;

        public boolean addAfter(Ordering ordering) {
            return after.add(ordering);
        }

        public boolean addBefore(Ordering ordering) {
            return before.add(ordering);
        }

        public void validate() {
            isBefore(new Ordering());
            isAfter(new Ordering());
        }

        /**
         * Check (recursively) if a fragment is before the specified fragment.
         *
         * @param ordering
         * @return
         */
        public boolean isBefore(Ordering ordering) {
            return isBeforeInternal(ordering, new HashSet<Ordering>());
        }

        protected boolean isBeforeInternal(Ordering ordering, Set<Ordering> checked) {
            checked.add(this);
            if (before.contains(ordering)) {
                return true;
            }
            Iterator<Ordering> beforeIterator = before.iterator();
            while (beforeIterator.hasNext()) {
                Ordering check = beforeIterator.next();
                if (checked.contains(check)) {
                    throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrdering(this.ordering.getJar()));
                }
                if (check.isBeforeInternal(ordering, checked)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check (recursively) if a fragment is after the specified fragment.
         *
         * @param ordering
         * @return
         */
        public boolean isAfter(Ordering ordering) {
            return isAfterInternal(ordering, new HashSet<Ordering>());
        }

        protected boolean isAfterInternal(Ordering ordering, Set<Ordering> checked) {
            checked.add(this);
            if (after.contains(ordering)) {
                return true;
            }
            Iterator<Ordering> afterIterator = after.iterator();
            while (afterIterator.hasNext()) {
                Ordering check = afterIterator.next();
                if (checked.contains(check)) {
                    throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrdering(this.ordering.getJar()));
                }
                if (check.isAfterInternal(ordering, checked)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check is a fragment marked as before others is after a fragment that
         * is not.
         *
         * @return true if a fragment marked as before others is after a
         *         fragment that is not
         */
        public boolean isLastBeforeOthers() {
            if (!beforeOthers) {
                throw new IllegalStateException();
            }
            Iterator<Ordering> beforeIterator = before.iterator();
            while (beforeIterator.hasNext()) {
                Ordering check = beforeIterator.next();
                if (!check.beforeOthers) {
                    return true;
                } else if (check.isLastBeforeOthers()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check is a fragment marked as after others is before a fragment that
         * is not.
         *
         * @return true if a fragment marked as after others is before a
         *         fragment that is not
         */
        public boolean isFirstAfterOthers() {
            if (!afterOthers) {
                throw new IllegalStateException();
            }
            Iterator<Ordering> afterIterator = after.iterator();
            while (afterIterator.hasNext()) {
                Ordering check = afterIterator.next();
                if (!check.afterOthers) {
                    return true;
                } else if (check.isFirstAfterOthers()) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Generate the Jar processing order.
     *
     * @param webOrderings The list of orderings, as parsed from the fragments
     * @param order        The generated order list
     */
    protected static void resolveOrder(List<WebOrdering> webOrderings, List<String> order) {
        List<Ordering> work = new ArrayList<Ordering>();

        // Populate the work Ordering list
        Iterator<WebOrdering> webOrderingsIterator = webOrderings.iterator();
        while (webOrderingsIterator.hasNext()) {
            WebOrdering webOrdering = webOrderingsIterator.next();
            Ordering ordering = new Ordering();
            ordering.ordering = webOrdering;
            ordering.afterOthers = webOrdering.isAfterOthers();
            ordering.beforeOthers = webOrdering.isBeforeOthers();
            if (ordering.afterOthers && ordering.beforeOthers) {
                // Cannot be both after and before others
                throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingBeforeAndAfter(webOrdering.getJar()));
            }
            work.add(ordering);
        }

        // Create double linked relationships between the orderings,
        // and resolve names
        Iterator<Ordering> workIterator = work.iterator();
        while (workIterator.hasNext()) {
            Ordering ordering = workIterator.next();
            WebOrdering webOrdering = ordering.ordering;
            Iterator<String> after = webOrdering.getAfter().iterator();
            while (after.hasNext()) {
                String name = after.next();
                Iterator<Ordering> workIterator2 = work.iterator();
                boolean found = false;
                while (workIterator2.hasNext()) {
                    Ordering ordering2 = workIterator2.next();
                    if (name.equals(ordering2.ordering.getName())) {
                        if (found) {
                            // Duplicate name
                            throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingDuplicateName(webOrdering.getJar()));
                        }
                        ordering.addAfter(ordering2);
                        ordering2.addBefore(ordering);
                        found = true;
                    }
                }
                if (!found) {
                    // Unknown name
                    UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingUnknownName(webOrdering.getJar());
                }
            }
            Iterator<String> before = webOrdering.getBefore().iterator();
            while (before.hasNext()) {
                String name = before.next();
                Iterator<Ordering> workIterator2 = work.iterator();
                boolean found = false;
                while (workIterator2.hasNext()) {
                    Ordering ordering2 = workIterator2.next();
                    if (name.equals(ordering2.ordering.getName())) {
                        if (found) {
                            // Duplicate name
                            throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingDuplicateName(webOrdering.getJar()));
                        }
                        ordering.addBefore(ordering2);
                        ordering2.addAfter(ordering);
                        found = true;
                    }
                }
                if (!found) {
                    // Unknown name
                    UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingUnknownName(webOrdering.getJar());
                }
            }
        }

        // Validate ordering
        workIterator = work.iterator();
        while (workIterator.hasNext()) {
            workIterator.next().validate();
        }

        // Create three ordered lists that will then be merged
        List<Ordering> tempOrder = new ArrayList<Ordering>();

        // Create the ordered list of fragments which are before others
        workIterator = work.iterator();
        while (workIterator.hasNext()) {
            Ordering ordering = workIterator.next();
            if (ordering.beforeOthers) {
                // Insert at the first possible position
                int insertAfter = -1;
                boolean last = ordering.isLastBeforeOthers();
                int lastBeforeOthers = -1;
                for (int i = 0; i < tempOrder.size(); i++) {
                    if (ordering.isAfter(tempOrder.get(i))) {
                        insertAfter = i;
                    }
                    if (tempOrder.get(i).beforeOthers) {
                        lastBeforeOthers = i;
                    }
                }
                int pos = insertAfter;
                if (last && lastBeforeOthers > insertAfter) {
                    pos = lastBeforeOthers;
                }
                tempOrder.add(pos + 1, ordering);
            } else if (ordering.afterOthers) {
                // Insert at the last possible element
                int insertBefore = tempOrder.size();
                boolean first = ordering.isFirstAfterOthers();
                int firstAfterOthers = tempOrder.size();
                for (int i = tempOrder.size() - 1; i >= 0; i--) {
                    if (ordering.isBefore(tempOrder.get(i))) {
                        insertBefore = i;
                    }
                    if (tempOrder.get(i).afterOthers) {
                        firstAfterOthers = i;
                    }
                }
                int pos = insertBefore;
                if (first && firstAfterOthers < insertBefore) {
                    pos = firstAfterOthers;
                }
                tempOrder.add(pos, ordering);
            } else {
                // Insert according to other already inserted elements
                int insertAfter = -1;
                int insertBefore = tempOrder.size();
                for (int i = 0; i < tempOrder.size(); i++) {
                    if (ordering.isAfter(tempOrder.get(i)) || tempOrder.get(i).beforeOthers) {
                        insertAfter = i;
                    }
                    if (ordering.isBefore(tempOrder.get(i)) || tempOrder.get(i).afterOthers) {
                        insertBefore = i;
                    }
                }
                if (insertAfter > insertBefore) {
                    // Conflicting order (probably caught earlier)
                    throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.invalidRelativeOrderingConflict(ordering.ordering.getJar()));
                }
                // Insert somewhere in the range
                tempOrder.add(insertAfter + 1, ordering);
            }
        }

        // Create the final ordered list
        Iterator<Ordering> tempOrderIterator = tempOrder.iterator();
        while (tempOrderIterator.hasNext()) {
            Ordering ordering = tempOrderIterator.next();
            order.add(ordering.ordering.getJar());
        }

    }

}
