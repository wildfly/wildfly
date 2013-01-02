package org.jboss.as.security.test;

import static java.util.Collections.addAll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.as.security.vault.MockRuntimeVaultReader;
import org.jboss.as.security.vault.VaultTool;
import org.jboss.as.server.services.security.VaultReaderException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VaultToolTestCase {

  private static final String KEYSTORE_PASSWORD = "mypassword";
  private static final String BLOCK_NAME = "myblock";
  private static final String ATTRIBUTE_NAME = "the_attribute_I_want_to_store";
  private static final String VALUE_TO_STORE = "the_value";
  private static final String ENC_FILE_DIR_VALUE = System.getProperty("java.io.tmpdir") + "/tmp/vault/";
  private static final String KEYSTORE_ALIAS_VALUE = "vault";
  private static final String ITERATION_COUNT_VALUE = "12";
  private static final String CODE_LOCATION = VaultToolTestCase.class.getProtectionDomain().getCodeSource().getLocation().getFile();
  private static final String KEYSTORE_URL_VALUE = CODE_LOCATION + "org/jboss/as/security/test/vault.keystore";
  private static final String MASKED_MYPASSWORD_VALUE = "MASK-UWB5tlhOmKYzJVl9KZaPN";
  private static final String SALT_VALUE = "bdfbdf12";
  private static final ByteArrayOutputStream SYSTEM_OUT = new ByteArrayOutputStream();

  @Test
  public void testVaultTool() throws IOException, VaultReaderException {
    // Replaces standard output with a ByteArrayOutputStream to parse the ouput later.
    System.setOut(new PrintStream(SYSTEM_OUT));
    try {
      String[] args = generateArgs(); // Generate the required arguments
      VaultTool.main(args);
    } catch (ExitException e) {
      Assert.assertEquals("Exit status is equal to 0", 0, e.status);
    }
    SYSTEM_OUT.flush();
    String ouput = new String(SYSTEM_OUT.toByteArray());
    String[] outputLines = ouput.split(System.getProperty("line.separator"));

    String vaultSharedKey = getStoredAttributeSharedKey(outputLines);
<<<<<<< HEAD
    Assert.assertNotNull("VaultTool did not return a line starting with VAULT::", vaultSharedKey);
=======
    Assert.assertNotNull("VaultTool returned a line starting with VAULT::", vaultSharedKey);
>>>>>>> AS7-4654: Reopened: Instead of storing attribute's value in the vault,

    MockRuntimeVaultReader rvr = new MockRuntimeVaultReader();
    Map<String, Object> options = generateVaultOptionsMap();
    rvr.createVault("", options);
    String retrievedValueFromVault = rvr.retrieveFromVault(vaultSharedKey);
<<<<<<< HEAD
    Assert.assertEquals("The value retrieved from vault is not the same as the one initially stored", VALUE_TO_STORE,
        retrievedValueFromVault);
=======
    Assert.assertEquals("The value retrieved from vault is the same as the one initially stored", VALUE_TO_STORE, retrievedValueFromVault);
>>>>>>> AS7-4654: Reopened: Instead of storing attribute's value in the vault,
  }

  private Map<String, Object> generateVaultOptionsMap() {
    Map<String, Object> options = new HashMap<String, Object>();
    options.put("KEYSTORE_URL", KEYSTORE_URL_VALUE);
    options.put("KEYSTORE_PASSWORD", MASKED_MYPASSWORD_VALUE);
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

  private String[] generateArgs() {
    List<String> args = new ArrayList<String>();
    addAll(args, "-k", "/tmp/vault.keystore");
<<<<<<< HEAD
    // addAll(args, "-v", KEYSTORE_ALIAS_VALUE);
=======
>>>>>>> AS7-4654: Reopened: Instead of storing attribute's value in the vault,
    addAll(args, "-p", KEYSTORE_PASSWORD);
    addAll(args, "-e", ENC_FILE_DIR_VALUE);
    addAll(args, "-s", SALT_VALUE);
    addAll(args, "-i", ITERATION_COUNT_VALUE);
    addAll(args, "-b", BLOCK_NAME);
    addAll(args, "-a", ATTRIBUTE_NAME);
    addAll(args, "-x", VALUE_TO_STORE);
    return args.toArray(new String[0]);
  }

  protected static class ExitException extends SecurityException {

    public final int status;

    public ExitException(int status) {
      super("There is no escape!");
      this.status = status;
    }
  }
  private static class NoExitSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
      // allow anything.
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
      // allow anything.
    }

    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }
  }

  @Before
  public void setUp() throws Exception {
    System.setSecurityManager(new NoExitSecurityManager());
  }

  @After
  public void tearDown() throws Exception {
    System.setSecurityManager(null); // or save and restore original
  }

}
