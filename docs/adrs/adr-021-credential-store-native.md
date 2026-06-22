# ADR-021: Credential Store Using Platform-Native Key Management

## Status
Accepted

## Context
API keys and secrets must be stored securely on device. Options include: SharedPreferences (plaintext - unacceptable), encrypted local files, platform keychain/keystore, or remote secret management.

## Decision
The default CredentialStore uses the platform's native key management:
- Android: EncryptedSharedPreferences with AndroidKeyStore system backing
- iOS: SecItemAdd/SecItemCopyMatching (Keychain Services)
- JVM: Java KeyStore with system-level protection

Credentials are never stored in application code, configuration files, or environment variables accessible to the application. Remote secret managers (HashiCorp Vault, AWS Secrets Manager) are supported as CredentialProvider implementations for enterprise deployments.

## Alternatives Considered
- Environment variables: Not suitable for mobile apps; accessible to other processes
- Build-time constants: Embedded in APK/IPA; extractable by decompilation
- App config files: No encryption; accessible on rooted devices
- Custom encryption: Reinventing platform security primitives is always a mistake

## Tradeoffs
- Pros:
  - Leverages platform-hardened security (hardware security modules on modern devices)
  - Keys survive app updates but can be invalidated on security events (e.g., biometric change)
  - Keys never exist in process memory as raw strings
- Cons:
  - Platform API differences require expect/actual implementations
  - Keychain access may require user authentication on first access (configurable)

## Consequences
Credential access is always asynchronous: suspend fun getCredential(key: String): Credential. Credentials are typed value classes (ApiKey, BearerToken, OAuth2Token) rather than raw strings. The credential store emits audit events on all access operations.