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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.wildfly.extension.security.manager.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.security.PermissionFactory;
import org.jboss.vfs.VirtualFile;

/**
 * This class implements a {@code DeploymentUnitProcessor} that parses security permission files that might be
 * included in application components.
 * <p/>
 * The EE7 specification (section EE6.2.2.6) allows application components to specify required security permissions:
 * <p/>
 * "<i>Permission declarations must be stored in META-INF/permissions.xml file within an EJB, web, application client, or
 * resource adapter archive in order for them to be located and processed.
 * <p/>
 * The permissions for a packaged library are the same as the permissions for the module. Thus, if a library is packaged
 * in a .war file, it gets the permissions of the .war file.
 * <p/>
 * For applications packaged in an .ear file, the declaration of permissions must be at .ear file level. This permission
 * set is applied to all modules and libraries packaged within  the .ear file or within its contained modules. Any
 * permissions.xml files within such packaged modules are ignored, regardless of whether a permissions.xml file has been
 * supplied for the .ear file itself.</i>"
 * <p/>
 * As can be noted, the EE spec doesn't allow sub-deployments to override permissions set at the .ear level. We find it
 * a bit too restrictive, so we introduced the META-INF/jboss-permissions.xml descriptor. It uses the same schema as the
 * standard permissions.xml file but, unlike the latter, is always processed and the permissions contained in it override
 * any permissions set by a parent deployment. If a deployment contains both permissions files, jboss-permissions.xml
 * takes precedence over the standard permissions.xml.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class PermissionsParseProcessor implements DeploymentUnitProcessor {

    private static final String PERMISSIONS_XML = "META-INF/permissions.xml";

    private static final String JBOSS_PERMISSIONS_XML = "META-INF/jboss-permissions.xml";

    // minimum set of permissions that are to be granted to all deployments.
    private final List<PermissionFactory> minPermissions;

    // maximum set of permissions deployments should have.
    private final List<PermissionFactory> maxPermissions;

    /**
     * Creates an instance of {@link PermissionsParseProcessor} with the specified minimum and maximum set of permissions.
     *
     * @param minPermissions a {@link List} containing the permissions that are to be granted to all deployments.
     * @param maxPermissions a {@link List} containing the maximum set of permissions a deployment can have. In other words,
     *                       all permissions in the minimum set plus the permissions parsed in META-INF/permissions.xml
     *                       must be implied by the maximum set.
     */
    public PermissionsParseProcessor(List<PermissionFactory> minPermissions, List<PermissionFactory> maxPermissions) {
        this.minPermissions = minPermissions;
        this.maxPermissions = maxPermissions;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);

        // non-spec behavior: always process permissions declared in META-INF/jboss-permissions.xml.
        VirtualFile jbossPermissionsXML = deploymentRoot.getRoot().getChild(JBOSS_PERMISSIONS_XML);
        if (jbossPermissionsXML.exists() && jbossPermissionsXML.isFile()) {
            List<PermissionFactory> factories = this.parsePermissions(jbossPermissionsXML, moduleLoader, moduleIdentifier);
            for (PermissionFactory factory : factories) {
                moduleSpecification.addPermissionFactory(factory);
            }
            // add the permissions specified in the minimum set.
            for (PermissionFactory factory : this.minPermissions) {
                moduleSpecification.addPermissionFactory(factory);
            }
        }

        // spec compliant behavior: only top-level deployments are processed (sub-deployments inherit permissions
        // defined at the .ear level, if any).
        else {
            if (deploymentUnit.getParent() == null) {
                VirtualFile permissionsXML = deploymentRoot.getRoot().getChild(PERMISSIONS_XML);
                if (permissionsXML.exists() && permissionsXML.isFile()) {
                    // parse the permissions and attach them in the deployment unit.
                    List<PermissionFactory> factories = this.parsePermissions(permissionsXML, moduleLoader, moduleIdentifier);
                    for (PermissionFactory factory : factories) {
                        moduleSpecification.addPermissionFactory(factory);
                    }
                }
                // add the minimum set of permissions to top-level deployments - sub-deployments will inherit them automatically.
                for (PermissionFactory factory : this.minPermissions) {
                    moduleSpecification.addPermissionFactory(factory);
                }
            } else {
                ModuleSpecification parentSpecification = deploymentUnit.getParent().getAttachment(Attachments.MODULE_SPECIFICATION);
                List<PermissionFactory> factories = parentSpecification.getPermissionFactories();
                if (factories != null && factories.size() > 0) {
                    // parent deployment contains permissions: subdeployments inherit those permissions.
                    for (PermissionFactory factory : factories) {
                        moduleSpecification.addPermissionFactory(factory);
                    }
                }
            }
        }


        // TODO validate the resulting set of permissions against the maximum set
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    /**
     * <p>
     * Parses the permissions declared in the specified file. The permissions are wrapped in factory objects so they can
     * be lazily instantiated after the deployment unit module has been created.
     * </p>
     *
     * @param file the {@link VirtualFile} that contains the permissions declarations.
     * @param loader the {@link ModuleLoader} that is to be used by the factory to instantiate the permission.
     * @param identifier the {@link ModuleIdentifier} that is to be used by the factory to instantiate the permission.
     * @return a list of {@link PermissionFactory} objects representing the parsed permissions.
     * @throws DeploymentUnitProcessingException if an error occurs while parsing the permissions.
     */
    private List<PermissionFactory> parsePermissions(final VirtualFile file, final ModuleLoader loader, final ModuleIdentifier identifier)
            throws DeploymentUnitProcessingException {

        InputStream inputStream = null;
        try {
            inputStream = file.openStream();
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(inputStream);
            return PermissionsParser.parse(xmlReader, loader, identifier);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e.getMessage(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
        }
    }
}