/*
Copyright 2018 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.test.integration.management.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.DefaultResourceNames;
import org.jboss.as.cli.impl.aesh.cmd.security.model.ElytronUtil;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 *
 * @author jdenise@redhat.com
 */
@RunWith(Arquillian.class)
public class SecurityAuthCommandsTestCase {

    private static final ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();

    private static CommandContext ctx;

    @ClassRule
    public static final TemporaryFolder temporaryUserHome = new TemporaryFolder();

    private static final String TEST_DOMAIN = "test-domain";
    private static final String TEST_HTTP_FACTORY = "test-http-factory";
    private static final String TEST_USERS_REALM = "test-users-realm";
    private static final String TEST_KS_REALM = "test-ks-realm";
    private static final String TEST_FS_REALM = "test-fs-realm";
    private static final String TEST_KS = "test-ks";

    private static final String TEST_UNDERTOW_DOMAIN = "test-undertow-security-domain";

    private static List<ModelNode> originalPropertiesRealms;
    private static List<ModelNode> originalKSRealms;
    private static List<ModelNode> originalHttpFactories;
    private static List<ModelNode> originalSecurityDomains;
    private static List<ModelNode> originalFSRealms;
    private static List<ModelNode> originalConstantMappers;
    private static List<ModelNode> originalConstantRoleMappers;

    @BeforeClass
    public static void setup() throws Exception {
        // Create ctx, used to setup the test and do the final reload.
        CommandContextConfiguration.Builder configBuilder = new CommandContextConfiguration.Builder();
        configBuilder.setConsoleOutput(consoleOutput).setInitConsole(true).
                setController("remote+http://" + TestSuiteEnvironment.getServerAddress()
                        + ":" + TestSuiteEnvironment.getServerPort());
        ctx = CommandContextFactory.getInstance().newCommandContext(configBuilder.build());
        ctx.connectController();
        // filesystem realm.
        addFSRealm();

        originalFSRealms = getFileSystemRealms();
        originalPropertiesRealms = getPropertiesRealm();
        originalHttpFactories = getHttpFactories();
        originalSecurityDomains = getSecurityDomains();
        originalConstantMappers = getConstantRealmMappers();
        originalConstantRoleMappers = getConstantRoleMappers();
        originalKSRealms = getKSRealms();
    }

    private static String escapePath(String filePath) {
        if (Util.isWindows()) {
            StringBuilder builder = new StringBuilder();
            for (char c : filePath.toCharArray()) {
                if (c == '\\') {
                    builder.append('\\');
                }
                builder.append(c);
            }
            return builder.toString();
        } else {
            return filePath;
        }
    }

    private static void addFSRealm() throws Exception {
        ctx.handle("/subsystem=elytron/filesystem-realm=" + TEST_FS_REALM + ":add(path="
                + escapePath(temporaryUserHome.newFolder("identities").getAbsolutePath()));
        ctx.handle("/subsystem=elytron/filesystem-realm=" + TEST_FS_REALM + ":add-identity(identity=user1");
        ctx.handle("/subsystem=elytron/filesystem-realm=" + TEST_FS_REALM + ":set-password(identity=user1,clear={password=mypassword})");
    }

    private static List<ModelNode> getPropertiesRealm() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("properties-realm");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/properties-realm=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static List<ModelNode> getKSRealms() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("key-store-realm");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/key-store-realm=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static List<ModelNode> getFileSystemRealms() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("filesystem-realm");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/filesystem-realm=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static List<String> getNames(String childrenType) throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set(childrenType);
        List<String> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            res.add(mn.asString());
        }
        return res;
    }

    private static List<ModelNode> getHttpFactories() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("http-authentication-factory");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/http-authentication-factory=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static List<ModelNode> getSecurityDomains() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("security-domain");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/security-domain=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static List<ModelNode> getConstantRealmMappers() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("constant-realm-mapper");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/constant-realm-mapper=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }

    private static void checkState() throws Exception {
        Assert.assertEquals(originalConstantMappers, getConstantRealmMappers());
        Assert.assertEquals(originalFSRealms, getFileSystemRealms());
        Assert.assertEquals(originalHttpFactories, getHttpFactories());
        Assert.assertEquals(originalPropertiesRealms, getPropertiesRealm());
        Assert.assertEquals(originalKSRealms, getKSRealms());
        Assert.assertEquals(originalSecurityDomains, getSecurityDomains());
        Assert.assertEquals(originalConstantRoleMappers, getConstantRoleMappers());
    }

    @After
    public void cleanupTest() throws Exception {
        try {
            eraseAllDomains();
            eraseAuth();
            checkState();
        } finally {
            ctx.handle("reload");
        }
    }

    private void eraseAuth() throws Exception {

        try {
            ModelNode op = createOpNode("subsystem=elytron/http-authentication-factory=" + TEST_HTTP_FACTORY,
                    "remove");
            executeForResult(op);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode op = createOpNode("subsystem=elytron/security-domain=" + TEST_DOMAIN,
                    "remove");
            executeForResult(op);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode op = createOpNode("subsystem=elytron/properties-realm=" + TEST_USERS_REALM,
                    "remove");
            executeForResult(op);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode op = createOpNode("subsystem=elytron/key-store-realm=" + TEST_KS_REALM,
                    "remove");
            executeForResult(op);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-realm-mapper=" + TEST_USERS_REALM,
                    "remove");
            executeForResult(removeMapper);
        } catch (Exception ex) {
            // OK,
        }
        try {
            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-realm-mapper=" + TEST_FS_REALM,
                    "remove");
            executeForResult(removeMapper);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-realm-mapper=" + TEST_KS_REALM,
                    "remove");
            executeForResult(removeMapper);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-role-mapper="
                    + DefaultResourceNames.ROLE_MAPPER_NAME,
                    "remove");
            executeForResult(removeMapper);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode removeMapper = createOpNode("subsystem=elytron/constant-realm-mapper=ApplicationRealm",
                    "remove");
            executeForResult(removeMapper);
        } catch (Exception ex) {
            // OK,
        }

        try {
            ModelNode op = createOpNode("subsystem=elytron/key-store=" + TEST_KS,
                    "remove");
            executeForResult(op);
        } catch (Exception ex) {
            // OK,
        }
    }

    private static void eraseAllDomains() throws Exception {
        Exception e = null;
        try {
            if (domainExists(TEST_UNDERTOW_DOMAIN)) {
                ModelNode eraseHttp = createOpNode("subsystem=undertow/application-security-domain=" + TEST_UNDERTOW_DOMAIN,
                        "remove");
                executeForResult(eraseHttp);
            }
        } catch (Exception ex) {
            if (e == null) {
                e = ex;
            }
        }
        if (e != null) {
            throw e;
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Exception e = null;
        if (ctx != null) {
            try {
                ctx.handle("/subsystem=elytron/filesystem-realm=" + TEST_FS_REALM + ":remove");
            } catch (Exception ex) {
                if (e == null) {
                    e = ex;
                }
            } finally {
                try {
                    ctx.handle("reload");
                } finally {
                    ctx.terminateSession();
                }
            }
        }
        if (e != null) {
            throw e;
        }
    }

    @Test
    public void testOOBHTTP() throws Exception {
        ctx.handle("security enable-http-auth-http-server --no-reload --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals(ElytronUtil.OOTB_APPLICATION_DOMAIN, getReferencedSecurityDomain(ctx, TEST_UNDERTOW_DOMAIN));
        ctx.handle("security disable-http-auth-http-server --no-reload --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertFalse(domainExists(TEST_UNDERTOW_DOMAIN));
    }

    @Test
    public void testOOBHTTP2() throws Exception {
        // New factory but reuse OOB ApplicationRealm properties realm.
        // side effect is to create a constant realm mapper for ApplicationRealm.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=BASIC "
                + "--user-properties-file=application-users.properties --group-properties-file=application-roles.properties  "
                + "--relative-to=jboss.server.config.dir --exposed-realm=ApplicationRealm --new-security-domain-name=" + TEST_DOMAIN
                + " --new-auth-factory-name=" + TEST_HTTP_FACTORY + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals(TEST_HTTP_FACTORY, getSecurityDomainAuthFactory(ctx, TEST_UNDERTOW_DOMAIN));

        // Check Realm.
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals("ApplicationRealm", getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        getNames(Util.CONSTANT_REALM_MAPPER).contains("ApplicationRealm");

        // Replace with file system realm.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=BASIC "
                + "--file-system-realm-name=" + TEST_FS_REALM + " --security-domain=" + TEST_UNDERTOW_DOMAIN);

        Assert.assertEquals(TEST_FS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        getNames(Util.CONSTANT_REALM_MAPPER).contains(TEST_FS_REALM);

        // check that domain has both realms.
        List<String> realms = getDomainRealms(TEST_DOMAIN);
        Assert.assertTrue(realms.contains("ApplicationRealm"));
        Assert.assertTrue(realms.contains(TEST_FS_REALM));
    }

    @Test
    public void testReferencedSecurityDomainHTTP() throws Exception {
        ctx.handle("security enable-http-auth-http-server --no-reload --security-domain=" + TEST_UNDERTOW_DOMAIN
                + " --referenced-security-domain=" + ElytronUtil.OOTB_APPLICATION_DOMAIN);
        Assert.assertEquals(ElytronUtil.OOTB_APPLICATION_DOMAIN, getReferencedSecurityDomain(ctx, TEST_UNDERTOW_DOMAIN));
        ctx.handle("security disable-http-auth-http-server --no-reload --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertFalse(domainExists(TEST_UNDERTOW_DOMAIN));
    }

    @Test
    public void testCompletion() throws Exception {
        {
            String cmd = "security enable-http-auth-http-server ";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            List<String> res = Arrays.asList("--no-reload", "--security-domain=");
            assertEquals(candidates.toString(), res, candidates);
            candidates = complete(ctx, cmd, null);
            assertEquals(candidates.toString(), res, candidates);
        }

        {
            String cmd = "security enable-http-auth-http-server --security-domain=foo ";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            List<String> res = Arrays.asList("--mechanism=", "--no-reload",
                    "--referenced-security-domain=");
            assertEquals(candidates.toString(), res, candidates);
            candidates = complete(ctx, cmd, null);
            assertEquals(candidates.toString(), res, candidates);
        }

        {
            String cmd = "security enable-http-auth-http-server --security-domain=foo "
                    + "--mechanism=";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            List<String> res = Arrays.asList("BASIC", "CLIENT_CERT", "DIGEST", "FORM");
            assertTrue(candidates.toString(), candidates.containsAll(res));
            candidates = complete(ctx, cmd, null);
            assertTrue(candidates.toString(), candidates.containsAll(res));
        }

        {
            String cmd = "security enable-http-auth-http-server --security-domain=foo "
                    + "--mechanism=BASIC ";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            assertFalse(candidates.toString(), candidates.contains("--referenced-security-domain="));
            candidates = complete(ctx, cmd, null);
            assertFalse(candidates.toString(), candidates.contains("--referenced-security-domain="));
        }

        {
            String cmd = "security enable-http-auth-http-server --security-domain=foo "
                    + "--referenced-security-domain=";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            List<String> res = Arrays.asList("ApplicationDomain");
            assertTrue(candidates.toString(), candidates.containsAll(res));
            candidates = complete(ctx, cmd, null);
            assertTrue(candidates.toString(), candidates.containsAll(res));
        }

        {
            String cmd = "security enable-http-auth-http-server --security-domain=foo "
                    + "--referenced-security-domain=ApplicationDomain ";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx,
                    cmd, cmd.length(), candidates);
            assertFalse(candidates.toString(), candidates.contains("--mechanism="));
            candidates = complete(ctx, cmd, null);
            assertFalse(candidates.toString(), candidates.contains("--mechanism="));
        }

    }
    @Test
    public void testHTTP() throws Exception {

        // Enable and add mechanisms.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=BASIC"
                + " --user-properties-file=application-users.properties --group-properties-file=application-roles.properties"
                + " --relative-to=jboss.server.config.dir --new-security-domain-name=" + TEST_DOMAIN
                + " --new-auth-factory-name=" + TEST_HTTP_FACTORY + " --new-realm-name="
                + TEST_USERS_REALM + " --exposed-realm=ApplicationRealm" + " --security-domain=" + TEST_UNDERTOW_DOMAIN);

        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_USERS_REALM));
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals(TEST_USERS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertTrue(getNames(Util.HTTP_AUTHENTICATION_FACTORY).contains(TEST_HTTP_FACTORY));
        Assert.assertTrue(getNames(Util.SECURITY_DOMAIN).contains(TEST_DOMAIN));

        Assert.assertEquals(Arrays.asList("BASIC"), getMechanisms(TEST_UNDERTOW_DOMAIN));

        // Add DIGEST.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=DIGEST"
                + " --properties-realm-name=" + TEST_USERS_REALM + " --exposed-realm=ApplicationRealm"
                + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals(TEST_USERS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).size() == 1);
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_USERS_REALM));
        // capture the state.
        List<ModelNode> factories = getHttpFactories();
        List<ModelNode> domains = getSecurityDomains();
        List<ModelNode> mappers = getConstantRealmMappers();
        List<ModelNode> userRealms = getPropertiesRealm();

        List<String> expected1 = Arrays.asList("BASIC", "DIGEST");
        Assert.assertEquals(expected1, getMechanisms(TEST_UNDERTOW_DOMAIN));

        Assert.assertTrue(getNames(Util.PROPERTIES_REALM).contains(TEST_USERS_REALM));

        // Disable digest mechanism
        ctx.handle("security disable-http-auth-http-server --no-reload --mechanism=DIGEST"
                + " --security-domain=" + TEST_UNDERTOW_DOMAIN);

        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).size() == 1);
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_USERS_REALM));
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals(TEST_USERS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals(domains, getSecurityDomains());
        //still secured
        Assert.assertEquals(TEST_HTTP_FACTORY, getSecurityDomainAuthFactory(ctx, TEST_UNDERTOW_DOMAIN));
        Assert.assertEquals(Arrays.asList("BASIC"), getMechanisms(TEST_UNDERTOW_DOMAIN));

        {
            // Disable the last mechanism is forbidden
            boolean failed = false;
            try {
                ctx.handle("security disable-http-auth-http-server --no-reload --mechanism=BASIC"
                        + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
            } catch (Exception ex) {
                // XXX OK.
                failed = true;
            }
            if (!failed) {
                throw new Exception("Disabling the last mechanism should have failed");
            }
        }

        // Re-enable the mechanism
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=DIGEST"
                + " --properties-realm-name=" + TEST_USERS_REALM + " --exposed-realm=ApplicationRealm" + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals(TEST_USERS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "BASIC"));
        Assert.assertEquals("ApplicationRealm", getExposedRealmName(TEST_UNDERTOW_DOMAIN, "DIGEST"));
        Assert.assertEquals(TEST_USERS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "DIGEST"));
        Assert.assertEquals(expected1, getMechanisms(TEST_UNDERTOW_DOMAIN));
        Assert.assertEquals(factories, getHttpFactories());
        Assert.assertEquals(domains, getSecurityDomains());
        Assert.assertEquals(mappers, getConstantRealmMappers());
        Assert.assertEquals(userRealms, getPropertiesRealm());
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).size() == 1);
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_USERS_REALM));
    }

    @Test
    public void testDisableAuth() throws Exception {
        boolean failed = false;
        try {
            ctx.handle("security disable-http-auth-http-server --no-reload" + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        } catch (Exception ex) {
            // XXX OK.
            failed = true;
        }
        if (!failed) {
            throw new Exception("Should have fail");
        }
    }

    @Test
    public void testHTTPCertificate() throws Exception {
        ctx.handle("/subsystem=elytron/key-store=" + TEST_KS + ":add(type=JKS, credential-reference={clear-text=pass})");
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=CLIENT_CERT"
                + " --key-store-name=" + TEST_KS + " --new-security-domain-name=" + TEST_DOMAIN
                + " --new-auth-factory-name=" + TEST_HTTP_FACTORY + " --new-realm-name=" + TEST_KS_REALM
                + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals(TEST_KS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "CLIENT_CERT"));
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_KS_REALM));

        // capture the state.
        List<ModelNode> factories = getHttpFactories();
        List<ModelNode> domains = getSecurityDomains();
        List<ModelNode> mappers = getConstantRealmMappers();
        List<ModelNode> ksRealms = getKSRealms();

        // Re-enable simply by re-using same key-store-realm, no changes expected.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=CLIENT_CERT"
                + " --key-store-realm-name=" + TEST_KS_REALM + " --security-domain=" + TEST_UNDERTOW_DOMAIN);
        Assert.assertEquals(TEST_KS_REALM, getMechanismRealmMapper(TEST_UNDERTOW_DOMAIN, "CLIENT_CERT"));
        Assert.assertTrue(getDomainRealms(TEST_DOMAIN).contains(TEST_KS_REALM));

        Assert.assertEquals(factories, getHttpFactories());
        Assert.assertEquals(domains, getSecurityDomains());
        Assert.assertEquals(mappers, getConstantRealmMappers());
        Assert.assertEquals(ksRealms, getKSRealms());

        ctx.handle("security disable-http-auth-http-server --no-reload --security-domain=" + TEST_UNDERTOW_DOMAIN);

        // Re-enable simply by re-using same key-store-realm with roles, no changes expected other than roles.
        ctx.handle("security enable-http-auth-http-server --no-reload --mechanism=CLIENT_CERT"
                + " --key-store-realm-name=" + TEST_KS_REALM + " --roles=FOO,BAR"
                + " --security-domain=" + TEST_UNDERTOW_DOMAIN);

        Assert.assertEquals(factories, getHttpFactories());
        Assert.assertEquals(mappers, getConstantRealmMappers());
        Assert.assertEquals(ksRealms, getKSRealms());

        Assert.assertEquals(DefaultResourceNames.ROLE_MAPPER_NAME, getRoleMapper(TEST_KS_REALM, TEST_DOMAIN));
        List<String> names = getNames(Util.CONSTANT_ROLE_MAPPER);
        Assert.assertTrue(names.toString(), names.contains(DefaultResourceNames.ROLE_MAPPER_NAME));
        ModelNode roleMapper = getConstantRoleMapper(DefaultResourceNames.ROLE_MAPPER_NAME);
        List<ModelNode> lst = roleMapper.get(Util.ROLES).asList();
        Assert.assertTrue(lst.size() == 2);
        for (ModelNode r : lst) {
            if (!r.asString().equals("FOO") && !r.asString().equals("BAR")) {
                throw new Exception("Invalid roles in " + lst);
            }
        }
    }

    private static List<String> getMechanisms(String securityDomain) throws Exception {
        String factory = getSecurityDomainAuthFactory(ctx, securityDomain);
        ModelNode mecs = createOpNode("subsystem=elytron/http-authentication-factory=" + factory,
                "read-attribute");
        mecs.get("name").set("mechanism-configurations");
        List<String> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(mecs).asList()) {
            res.add(mn.get("mechanism-name").asString());
        }
        return res;
    }

    private static String getExposedRealmName(String securityDomain, String mec) throws Exception {
        String factory = getSecurityDomainAuthFactory(ctx, securityDomain);
        ModelNode mecs = createOpNode("subsystem=elytron/http-authentication-factory=" + factory,
                "read-attribute");
        mecs.get("name").set("mechanism-configurations");
        for (ModelNode mn : executeForResult(mecs).asList()) {
            if (mn.get("mechanism-name").asString().equals(mec)) {
                return mn.get("mechanism-realm-configurations").asList().get(0).get("realm-name").asString();
            }
        }
        return null;
    }

    private static String getMechanismRealmMapper(String securityDomain, String mec) throws Exception {
        String factory = getSecurityDomainAuthFactory(ctx, securityDomain);
        ModelNode mecs = createOpNode("subsystem=elytron/http-authentication-factory=" + factory,
                "read-attribute");
        mecs.get("name").set("mechanism-configurations");
        for (ModelNode mn : executeForResult(mecs).asList()) {
            if (mn.get("mechanism-name").asString().equals(mec)) {
                return mn.get("realm-mapper").asString();
            }
        }
        return null;
    }

    private static List<String> getDomainRealms(String domain) throws Exception {
        ModelNode realms = createOpNode("subsystem=elytron/security-domain=" + domain,
                "read-attribute");
        realms.get("name").set("realms");
        List<String> lst = new ArrayList<>();
        for (ModelNode mn : executeForResult(realms).asList()) {
            lst.add(mn.get("realm").asString());
        }
        return lst;
    }

    private static boolean domainExists(String domain) throws Exception {
        ModelNode realms = createOpNode("subsystem=undertow", "read-children-names");
        realms.get("child-type").set("application-security-domain");
        List<ModelNode> mn = executeForResult(realms).asList();
        for (ModelNode c : mn) {
            if (c.asString().equals(domain)) {
                return true;
            }
        }
        return false;
    }

    private static String getSecurityDomainAuthFactory(CommandContext ctx, String domain) throws Exception {
        return readAttribute(ctx, domain, Util.HTTP_AUTHENTICATION_FACTORY);
    }

    private static String getReferencedSecurityDomain(CommandContext ctx, String domain) throws Exception {
        return readAttribute(ctx, domain, Util.SECURITY_DOMAIN);
    }

    private static String readAttribute(CommandContext ctx, String domain, String name) throws Exception {
        final DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        final ModelNode request;
        try {
            builder.setOperationName(Util.READ_ATTRIBUTE);
            builder.addNode(Util.SUBSYSTEM, Util.UNDERTOW);
            builder.addNode(Util.APPLICATION_SECURITY_DOMAIN, domain);
            builder.addProperty(Util.NAME, name);
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        final ModelNode outcome = ctx.getModelControllerClient().execute(request);
        if (Util.isSuccess(outcome)) {
            boolean hasResult = outcome.has(Util.RESULT);
            if (hasResult) {
                if (outcome.get(Util.RESULT).isDefined()) {
                    return outcome.get(Util.RESULT).asString();
                } else {
                    return null;
                }
            }
        }

        throw new Exception("Error retrieving Auth factory " + outcome);
    }

    private static String getRoleMapper(String realm, String securityDomain) throws Exception {
        ModelNode mecs = createOpNode("subsystem=elytron/security-domain=" + securityDomain,
                "read-attribute");
        mecs.get("name").set("realms");
        for (ModelNode mn : executeForResult(mecs).asList()) {
            if (mn.get("realm").asString().equals(realm)) {
                return mn.get("role-mapper").asString();
            }
        }
        return null;
    }

    private static ModelNode getConstantRoleMapper(String name) throws Exception {
        ModelNode prop = createOpNode("subsystem=elytron/constant-role-mapper=" + name, "read-resource");
        prop.get("recursive").set(Boolean.TRUE);
        prop.get("recursive-depth").set(100);
        return executeForResult(prop);
    }

    private static List<ModelNode> getConstantRoleMappers() throws Exception {
        ModelNode props = createOpNode("subsystem=elytron",
                "read-children-names");
        props.get("child-type").set("constant-role-mapper");
        List<ModelNode> res = new ArrayList<>();
        for (ModelNode mn : executeForResult(props).asList()) {
            ModelNode prop = createOpNode("subsystem=elytron/constant-role-mapper=" + mn.asString(), "read-resource");
            prop.get("recursive").set(Boolean.TRUE);
            prop.get("recursive-depth").set(100);
            res.add(executeForResult(prop));
        }
        return res;
    }


    private static ModelNode executeForResult(final ModelNode operation) throws Exception {
        try {
            final ModelNode result = ctx.getModelControllerClient().execute(operation);
            checkSuccessful(result, operation);
            return result.get(RESULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkSuccessful(final ModelNode result,
            final ModelNode operation) throws Exception {
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new Exception(result.get(
                    FAILURE_DESCRIPTION).toString());
        }
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    // This completion is what aesh-readline completion is calling, so more
    // similar to interactive CLI session
    private List<String> complete(CommandContext ctx, String cmd, Boolean separator) {
        Completion<AeshCompleteOperation> completer
                = (Completion<AeshCompleteOperation>) ctx.getDefaultCommandCompleter();
        AeshCompleteOperation op = new AeshCompleteOperation(cmd, cmd.length());
        completer.complete(op);
        if (separator != null) {
            assertEquals(op.hasAppendSeparator(), separator);
        }
        List<String> candidates = new ArrayList<>();
        for (TerminalString ts : op.getCompletionCandidates()) {
            candidates.add(ts.getCharacters());
        }
        // aesh-readline does sort the candidates prior to display.
        Collections.sort(candidates);
        return candidates;
    }
}
