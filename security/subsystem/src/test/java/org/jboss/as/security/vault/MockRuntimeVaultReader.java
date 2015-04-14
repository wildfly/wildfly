package org.jboss.as.security.vault;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.as.server.services.security.RuntimeVaultReader;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;

public class MockRuntimeVaultReader extends RuntimeVaultReader {

  private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

  protected SecurityVault vault;

  public void createVault(final String fqn, final Map<String, Object> options) throws VaultReaderException {
    Map<String, Object> vaultOptions = new HashMap<String, Object>(options);
    SecurityVault vault = null;
    try {
      vault = AccessController.doPrivileged(new PrivilegedExceptionAction<SecurityVault>() {

        @Override
        public SecurityVault run() throws Exception {
          if (fqn == null || fqn.isEmpty()) {
            return SecurityVaultFactory.get();
          } else {
            return SecurityVaultFactory.get(fqn);
          }
        }
      });
    } catch (PrivilegedActionException e) {
      Throwable t = e.getCause();
      if (t instanceof SecurityVaultException) {
        throw SecurityLogger.ROOT_LOGGER.vaultReaderException(t);
      }
      if (t instanceof RuntimeException) {
        throw SecurityLogger.ROOT_LOGGER.runtimeException(t);
      }
      throw SecurityLogger.ROOT_LOGGER.runtimeException(t);
    }
    try {
      vault.init(vaultOptions);
    } catch (SecurityVaultException e) {
      e.printStackTrace();
      throw SecurityLogger.ROOT_LOGGER.vaultReaderException(e);
    }
    this.vault = vault;
  }

  public String retrieveFromVault(final String password) throws SecurityException {
    if (isVaultFormat(password)) {

      if (vault == null) {
        throw SecurityLogger.ROOT_LOGGER.vaultNotInitializedException();
      }

      try {
        return getValueAsString(password);
      } catch (SecurityVaultException e) {
        throw SecurityLogger.ROOT_LOGGER.securityException(e);
      }

    }
    return password;
  }

  private String getValueAsString(String vaultString) throws SecurityVaultException {
    char[] val = getValue(vaultString);
    if (val != null)
      return new String(val);
    return null;
  }

  public boolean isVaultFormat(String str) {
    return str != null && VAULT_PATTERN.matcher(str).matches();
  }

  private char[] getValue(String vaultString) throws SecurityVaultException {
    String[] tokens = tokens(vaultString);
    return vault.retrieve(tokens[1], tokens[2], tokens[3].getBytes(VaultSession.CHARSET));
  }

  private String[] tokens(String vaultString) {
    StringTokenizer tokenizer = new StringTokenizer(vaultString, "::");
    int length = tokenizer.countTokens();
    String[] tokens = new String[length];

    int index = 0;
    while (tokenizer != null && tokenizer.hasMoreTokens()) {
      tokens[index++] = tokenizer.nextToken();
    }
    return tokens;
  }

}
