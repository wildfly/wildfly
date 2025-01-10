/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ExpressionStreamReaderDelegate;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.SimpleExpressionResolver;
import org.jboss.metadata.property.SystemPropertyResolver;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.security.PermissionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.security.manager.deployment.PermissionsParser;

/**
 * Unit test to verify that the permissions parser is able to use a property resolver to parse permissions.
 *
 * @author Yeray Borges
 */
public class PermissionParserExpressionsTestCase {
    final List<SimpleExpressionResolver> resolvers = Arrays.asList(SystemPropertyResolver.INSTANCE);
    final CompositePropertyResolver compositePropertyResolver = new CompositePropertyResolver(resolvers.toArray(new SimpleExpressionResolver[0]));
    final PropertyReplacer propertyReplacer = PropertyReplacers.resolvingExpressionReplacer(compositePropertyResolver);
    final String identifier = "java.base";
    final Path fileUnderTest = Paths.get("src", "test", "resources", "propertypermission", "permissions.xml");
    final Function<String, String> functionExpand = (value) -> propertyReplacer.replaceProperties(value);

    @Test
    public void test() throws DeploymentUnitProcessingException {
        System.setProperty("CLASS_NAME", "java.io.FilePermission");
        System.setProperty("NAME_A", "A");
        System.setProperty("NAME_B", "B");
        System.setProperty("NAME_C", "C");
        System.setProperty("ACTION_READ", "read");

        File[] modulePaths = getModulePaths();
        LocalModuleLoader ml = new LocalModuleLoader(modulePaths);

        List<PermissionFactory> permissionFactories = parsePermissions(fileUnderTest, ml, identifier, functionExpand);
        Assert.assertEquals("Unexpected number of permissions", 3, permissionFactories.size());
        Permission permission = permissionFactories.get(0).construct();
        Assert.assertNotNull(permission);

        Assert.assertTrue("Unexpected permission class", permission instanceof FilePermission);
        Assert.assertEquals("Unexpected permission name", permission.getName(), "A");
        Assert.assertEquals("Unexpected permission action", permission.getActions(), "read");

        permission = permissionFactories.get(1).construct();
        Assert.assertNotNull(permission);

        Assert.assertTrue("Unexpected permission class", permission instanceof FilePermission);
        Assert.assertEquals("Unexpected permission name", permission.getName(), "B");
        Assert.assertEquals("Unexpected permission action", permission.getActions(), "read");

        permission = permissionFactories.get(2).construct();
        Assert.assertNotNull(permission);

        Assert.assertTrue("Unexpected permission class", permission instanceof FilePermission);
        Assert.assertEquals("Unexpected permission name", permission.getName(), "C");
        Assert.assertEquals("Unexpected permission action", permission.getActions(), "write");
    }

    private List<PermissionFactory> parsePermissions(final Path path, final ModuleLoader loader, final String identifier, final Function<String, String> exprExpandFunction)
            throws DeploymentUnitProcessingException {

        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(path);
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            final ExpressionStreamReaderDelegate expressionStreamReaderDelegate = new ExpressionStreamReaderDelegate(inputFactory.createXMLStreamReader(inputStream), exprExpandFunction);
            return PermissionsParser.parse(expressionStreamReaderDelegate, loader, identifier);
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

    private File[] getModulePaths() {
        final List<File> files = new ArrayList<>();
        String modulePath = System.getProperty("module.path");
        if (modulePath == null) {
            fail("module.path system property is not set");
        }
        final String[] modulePaths = modulePath.split(Pattern.quote(File.pathSeparator));
        for (String path: modulePaths) {
            files.add(Paths.get(path).normalize().toFile());
        }

        return files.toArray(new File[]{});
    }
}
