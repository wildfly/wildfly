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

package org.jboss.as.security.vault;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.addAll;

public class VaultToolTestCase extends VaultTest {

  private static void createAndFillKeystore(String fileName) throws Exception {
      KeyStore ks = KeyStore.getInstance("JCEKS");
      ks.load(null, KEYSTORE_PASSWORD.toCharArray());
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(128);
      SecretKey secretKey = keyGenerator.generateKey();
      KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretKey);
      KeyStore.PasswordProtection p = new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray());
      ks.setEntry(KEYSTORE_ALIAS_VALUE, skEntry, p);
      ks.store(new FileOutputStream(fileName), KEYSTORE_PASSWORD.toCharArray());
  }

  @Test
  public void testVaultTool() throws Exception {
    doTestVaultTool(true, false);
  }

  @Test
  public void testVaultFallback() throws Exception {
    doTestVaultTool(true, true);
  }

  @Test
  public void testNoKeyStoreFile() throws Exception {
    doTestVaultTool(false, false);
  }

  private void doTestVaultTool(boolean prepareKeystore, boolean testFallbackFromIncorrectPasswordValue) throws Exception {

    if (prepareKeystore) {
        createAndFillKeystore(KEYSTORE_URL_VALUE);
    }

    // Replaces standard output with a ByteArrayOutputStream to parse the output later.
    System.setOut(new PrintStream(SYSTEM_OUT));

    try {
        String[] args = generateArgs(); // Generate the required arguments
        VaultTool.main(args);
    } catch (ExitException e) {
        Assert.assertEquals("Exit status is equal to 0", 0, e.status);
    }
    SYSTEM_OUT.flush();
    String ouput = new String(SYSTEM_OUT.toByteArray());
    String[] outputLines = ouput.split("\n");

    String vaultSharedKey = getStoredAttributeSharedKey(outputLines);
    Assert.assertNotNull("VaultTool did not return a line starting with VAULT::", vaultSharedKey);
    MockRuntimeVaultReader rvr = new MockRuntimeVaultReader();
    Map<String, Object> options = generateVaultOptionsMap(testFallbackFromIncorrectPasswordValue);
    rvr.createVault("", options);
    String retrievedValueFromVault = rvr.retrieveFromVault(vaultSharedKey);
    Assert.assertEquals("The value retrieved from vault is not the same as the one initially stored", VALUE_TO_STORE,
        retrievedValueFromVault);
  }

  private Map<String, Object> generateVaultOptionsMap(boolean testFallbackFromIncorrectPasswordValue) {
    Map<String, Object> options = new HashMap<String, Object>();
    options.put("KEYSTORE_URL", KEYSTORE_URL_VALUE);
    if (testFallbackFromIncorrectPasswordValue) {
        options.put("KEYSTORE_PASSWORD", MASKED_MYPASSWORD_VALUE_INCORRECT);
    } else {
        options.put("KEYSTORE_PASSWORD", MASKED_MYPASSWORD_VALUE);
    }
    options.put("SALT", SALT_VALUE);
    options.put("ITERATION_COUNT", ITERATION_COUNT_VALUE);
    options.put("KEYSTORE_ALIAS", KEYSTORE_ALIAS_VALUE);
    options.put("ENC_FILE_DIR", ENC_FILE_DIR_VALUE);
    return options;
  }

  private String getStoredAttributeSharedKey(String[] linesOfOutput) {
    String vaultSharedKey = null;
    for (String line : linesOfOutput) {
      if (line.startsWith("VAULT::")) {
        vaultSharedKey = line;
        break;
      }
    }
    return vaultSharedKey;
  }

  /**
   * Generates arguments to create security vault and store a password inside.
   * @return
   */
  private String[] generateArgs() {
    List<String> args = new ArrayList<String>();
    addAll(args, "-k", KEYSTORE_URL_VALUE);
    addAll(args, "-p", KEYSTORE_PASSWORD);
    addAll(args, "-e", ENC_FILE_DIR_VALUE);
    addAll(args, "-s", SALT_VALUE);
    addAll(args, "-i", ITERATION_COUNT_VALUE);
    addAll(args, "-b", BLOCK_NAME);
    addAll(args, "-a", ATTRIBUTE_NAME);
    addAll(args, "-x", VALUE_TO_STORE);
    addAll(args, "-t");
    return args.toArray(new String[0]);
  }

  @Before
  public void setUp() throws Exception {
    cleanDirectory(ENC_FILE_DIR_VALUE);
    System.setSecurityManager(new NoExitSecurityManager());
  }

  @After
  public void tearDown() throws Exception {
    System.setSecurityManager(null); // or save and restore original
    cleanDirectory(ENC_FILE_DIR_VALUE);
    File keyStoreFile = new File(KEYSTORE_URL_VALUE);
    if (keyStoreFile.exists()) {
        keyStoreFile.delete();
    }
  }
}
