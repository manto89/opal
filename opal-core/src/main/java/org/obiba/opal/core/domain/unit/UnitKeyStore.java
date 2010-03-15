/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.domain.unit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.vfs.FileObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.obiba.core.domain.AbstractEntity;
import org.obiba.opal.core.crypt.CacheablePasswordCallback;
import org.obiba.opal.core.crypt.CachingCallbackHandler;
import org.obiba.opal.core.crypt.KeyProviderException;
import org.obiba.opal.core.crypt.KeyProviderSecurityException;
import org.obiba.opal.core.unit.FunctionalUnit;
import org.springframework.util.Assert;

/**
 * A {@link FunctionalUnit}'s keystore.
 */
@Entity
@Table(name = "unit_key_store", uniqueConstraints = { @UniqueConstraint(columnNames = { "unit" }) })
public class UnitKeyStore extends AbstractEntity {
  //
  // Constants
  //

  private static final long serialVersionUID = 1L;

  private static final String PASSWORD_FOR = "Password for";

  //
  // Instance Variables
  //

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false, length = 1048576)
  private byte[] keyStore;

  @Transient
  KeyStore store;

  @Transient
  private CallbackHandler callbackHandler;

  //
  // Methods
  //

  public void setCallbackHander(CallbackHandler callbackHander) {
    this.callbackHandler = callbackHander;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public KeyStore getKeyStore() {
    KeyStore ks = null;
    try {
      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt(getPasswordFor(unit)).build();
      ks = loadKeyStore(passwordCallback);
    } catch(KeyStoreException e) {
      clearPasswordCache(callbackHandler, unit);
      throw new KeyProviderSecurityException("Wrong keystore password or keystore was tampered with");
    } catch(GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch(IOException ex) {
      clearPasswordCache(callbackHandler, unit);
      translateAndRethrowKeyStoreIOException(ex);
    } catch(UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    }

    return ks;
  }

  public void setKeyStore(KeyStore keyStore) {
    ByteArrayOutputStream b = new ByteArrayOutputStream();

    try {
      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt(getPasswordFor(unit)).build();
      keyStore.store(b, getKeyPassword(passwordCallback));
    } catch(KeyStoreException e) {
      clearPasswordCache(callbackHandler, unit);
      throw new KeyProviderSecurityException("Wrong keystore password or keystore was tampered with");
    } catch(GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch(IOException ex) {
      clearPasswordCache(callbackHandler, unit);
      translateAndRethrowKeyStoreIOException(ex);
    } catch(UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    }
    this.keyStore = b.toByteArray();
  }

  private char[] getKeyPassword(CacheablePasswordCallback passwordCallback) throws UnsupportedCallbackException, IOException {
    callbackHandler.handle(new CacheablePasswordCallback[] { passwordCallback });
    return passwordCallback.getPassword();
  }

  private KeyStore loadKeyStore(CacheablePasswordCallback passwordCallback) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnsupportedCallbackException {
    KeyStore ks;
    ks = KeyStore.getInstance("JCEKS");
    ks.load(new ByteArrayInputStream(keyStore), getKeyPassword(passwordCallback));
    return ks;
  }

  private static void translateAndRethrowKeyStoreIOException(IOException ex) {
    if(ex.getCause() != null && ex.getCause() instanceof UnrecoverableKeyException) {
      throw new KeyProviderSecurityException("Wrong keystore password");
    }
    throw new RuntimeException(ex);
  }

  //
  // Inner Classes
  //

  public static class Builder {
    private String unit;

    private CallbackHandler callbackHandler;

    public static Builder newStore() {
      return new Builder();
    }

    public Builder unit(String unit) {
      this.unit = unit;
      return this;
    }

    public Builder passwordPrompt(CallbackHandler callbackHandler) {
      this.callbackHandler = callbackHandler;
      return this;
    }

    private char[] getKeyPassword(CacheablePasswordCallback passwordCallback) throws UnsupportedCallbackException, IOException {
      callbackHandler.handle(new CacheablePasswordCallback[] { passwordCallback });
      return passwordCallback.getPassword();
    }

    public UnitKeyStore build() {
      Assert.hasText(unit, "unit must not be null or empty");
      Assert.notNull(callbackHandler, "callbackHander must not be null");

      UnitKeyStore.loadBouncyCastle();

      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt("Enter '" + unit + "' keystore password:  ").confirmation("Re-enter '" + unit + "' keystore password:  ").build();

      KeyStore keyStore = createEmptyKeyStore(passwordCallback);

      return createUnitKeyStore(keyStore);
    }

    private KeyStore createEmptyKeyStore(CacheablePasswordCallback passwordCallback) {
      KeyStore keyStore = null;
      try {
        keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, getKeyPassword(passwordCallback));
      } catch(KeyStoreException e) {
        clearPasswordCache(callbackHandler, unit);
        throw new KeyProviderSecurityException("Wrong keystore password or keystore was tampered with");
      } catch(GeneralSecurityException e) {
        throw new RuntimeException(e);
      } catch(IOException ex) {
        clearPasswordCache(callbackHandler, unit);
        translateAndRethrowKeyStoreIOException(ex);
      } catch(UnsupportedCallbackException e) {
        throw new RuntimeException(e);
      }
      return keyStore;
    }

    private UnitKeyStore createUnitKeyStore(KeyStore keyStore) {
      UnitKeyStore unitKeyStore = new UnitKeyStore();
      unitKeyStore.setUnit(unit);
      unitKeyStore.setCallbackHander(callbackHandler);
      unitKeyStore.setKeyStore(keyStore);
      return unitKeyStore;
    }
  }

  public static X509Certificate makeCertificate(PrivateKey issuerPrivateKey, PublicKey subjectPublicKey, String certificateInfo, String signatureAlgorithm) throws SignatureException, InvalidKeyException, CertificateEncodingException, NoSuchAlgorithmException {
    final org.bouncycastle.x509.X509V3CertificateGenerator certificateGenerator = new org.bouncycastle.x509.X509V3CertificateGenerator();
    final org.bouncycastle.asn1.x509.X509Name issuerDN = new org.bouncycastle.asn1.x509.X509Name(certificateInfo);
    final org.bouncycastle.asn1.x509.X509Name subjectDN = new org.bouncycastle.asn1.x509.X509Name(certificateInfo);
    final int daysTillExpiry = 30 * 365;

    final Calendar expiry = Calendar.getInstance();
    expiry.add(Calendar.DAY_OF_YEAR, daysTillExpiry);

    certificateGenerator.setSerialNumber(java.math.BigInteger.valueOf(System.currentTimeMillis()));
    certificateGenerator.setIssuerDN(issuerDN);
    certificateGenerator.setSubjectDN(subjectDN);
    certificateGenerator.setPublicKey(subjectPublicKey);
    certificateGenerator.setNotBefore(new Date());
    certificateGenerator.setNotAfter(expiry.getTime());
    certificateGenerator.setSignatureAlgorithm(signatureAlgorithm);

    return certificateGenerator.generate(issuerPrivateKey);
  }

  public void createOrUpdateKey(String alias, String algorithm, int size, String certificateInfo) {
    try {
      KeyPair keyPair = generateKeyPair(algorithm, size);
      X509Certificate cert = makeCertificate(algorithm, certificateInfo, keyPair);

      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt(getPasswordFor(unit)).build();

      KeyStore keyStore = getKeyStore();
      keyStore.setKeyEntry(alias, keyPair.getPrivate(), getKeyPassword(passwordCallback), new X509Certificate[] { cert });
      setKeyStore(keyStore);
    } catch(GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch(UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes the key associated with the provided alias.
   * @param alias key to delete
   */
  public void deleteKey(String alias) {
    KeyStore keyStore = getKeyStore();
    try {
      keyStore.deleteEntry(alias);
      setKeyStore(keyStore);
    } catch(KeyStoreException e) {
      throw new KeyProviderException(e);
    }

  }

  /**
   * Returns true if the provided alias exists.
   * @param alias check if this alias exists in the KeyStore.
   * @return true if the alias exists
   */
  public boolean aliasExists(String alias) {
    KeyStore keyStore = getKeyStore();
    try {
      return keyStore.containsAlias(alias);
    } catch(KeyStoreException e) {
      throw new KeyProviderException(e);
    }
  }

  public static void loadBouncyCastle() {
    if(java.security.Security.getProvider("BC") == null) java.security.Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Import a private key and it's associated certificate into the keystore at the given alias.
   * @param alias name of the key
   * @param privateKey private key in the PEM format
   * @param certificate certificate in the PEM format
   */
  public void importKey(String alias, FileObject privateKey, FileObject certificate) {
    Key key = getPrivateKeyFile(privateKey);
    X509Certificate cert = getCertificateFromFile(certificate);
    KeyStore keyStore = getKeyStore();
    CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt(getPasswordFor(alias)).build();
    try {
      keyStore.setKeyEntry(alias, key, getKeyPassword(passwordCallback), new X509Certificate[] { cert });
      setKeyStore(keyStore);
    } catch(KeyStoreException e) {
      throw new RuntimeException(e);
    } catch(UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Import a private key into the keystore and generate an associated certificate at the given alias.
   * @param alias name of the key
   * @param privateKey private key in the PEM format
   * @param certificateInfo Certificate attributes as a String (e.g. CN=Administrator, OU=Bioinformatics, O=GQ,
   * L=Montreal, ST=Quebec, C=CA)
   */
  public void importKey(String alias, FileObject privateKey, String certificateInfo) {
    KeyPair keyPair = getKeyPairFromFile(privateKey);
    X509Certificate cert;
    try {
      cert = UnitKeyStore.makeCertificate(keyPair.getPrivate(), keyPair.getPublic(), certificateInfo, chooseSignatureAlgorithm(keyPair.getPrivate().getAlgorithm()));
      KeyStore keyStore = getKeyStore();
      CacheablePasswordCallback passwordCallback = CacheablePasswordCallback.Builder.newCallback().key(unit).prompt(getPasswordFor(alias)).build();

      keyStore.setKeyEntry(alias, keyPair.getPrivate(), getKeyPassword(passwordCallback), new X509Certificate[] { cert });
      setKeyStore(keyStore);
    } catch(GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch(UnsupportedCallbackException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }

  }

  private X509Certificate makeCertificate(String algorithm, String certificateInfo, KeyPair keyPair) throws SignatureException, InvalidKeyException, CertificateEncodingException, NoSuchAlgorithmException {
    X509Certificate cert = UnitKeyStore.makeCertificate(keyPair.getPrivate(), keyPair.getPublic(), certificateInfo, chooseSignatureAlgorithm(algorithm));
    return cert;
  }

  private KeyPair generateKeyPair(String algorithm, int size) throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator;
    keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
    keyPairGenerator.initialize(size);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair;
  }

  private String chooseSignatureAlgorithm(String keyAlgorithm) {
    // TODO add more algorithms here.
    if(keyAlgorithm.equals("DSA")) {
      return "SHA1withDSA";
    }
    return "SHA1WithRSA";
  }

  private KeyPair getKeyPairFromFile(FileObject privateKey) {
    try {
      PEMReader pemReader = new PEMReader(new InputStreamReader(privateKey.getContent().getInputStream()), new PasswordFinder() {
        public char[] getPassword() {
          return System.console().readPassword("%s:  ", "Password for imported private key");
        }
      });
      Object object = pemReader.readObject();
      if(object == null) {
        throw new RuntimeException("The file [" + privateKey.getName() + "] does not contain a PEM file.");
      } else if(object instanceof KeyPair) {
        return (KeyPair) object;
      }
      throw new RuntimeException("Unexpected type [" + object + "]. Expected KeyPair.");
    } catch(FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Key getPrivateKeyFile(FileObject privateKey) {
    try {
      PEMReader pemReader = new PEMReader(new InputStreamReader(privateKey.getContent().getInputStream()), new PasswordFinder() {
        public char[] getPassword() {
          return System.console().readPassword("%s:  ", "Password for imported private key");
        }
      });
      Object pemObject = pemReader.readObject();
      if(pemObject == null) {
        throw new RuntimeException("The file [" + privateKey.getName() + "] does not contain a PEM file.");
      }
      return toPrivateKey(pemObject);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Key toPrivateKey(Object pemObject) {
    if(pemObject instanceof KeyPair) {
      KeyPair keyPair = (KeyPair) pemObject;
      return keyPair.getPrivate();
    } else if(pemObject instanceof Key) {
      return (Key) pemObject;
    }
    throw new RuntimeException("Unexpected type [" + pemObject + "]. Expected KeyPair or Key.");
  }

  private X509Certificate getCertificateFromFile(FileObject certificate) {
    try {
      PEMReader pemReader = new PEMReader(new InputStreamReader(certificate.getContent().getInputStream()), new PasswordFinder() {

        public char[] getPassword() {
          return System.console().readPassword("%s:  ", "Password for imported certificate");
        }
      });
      Object object = pemReader.readObject();
      if(object instanceof X509Certificate) {
        return (X509Certificate) object;
      }
      throw new RuntimeException("Unexpected type [" + object + "]. Expected X509Certificate.");
    } catch(FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void clearPasswordCache(CallbackHandler callbackHandler, String alias) {
    if(callbackHandler instanceof CachingCallbackHandler) {
      ((CachingCallbackHandler) callbackHandler).clearPasswordCache(alias);
    }
  }

  /**
   * Returns "Password for 'name':  ".
   */
  private String getPasswordFor(String name) {
    return new StringBuilder().append(PASSWORD_FOR).append(" '").append(name).append("':  ").toString();
  }
}
