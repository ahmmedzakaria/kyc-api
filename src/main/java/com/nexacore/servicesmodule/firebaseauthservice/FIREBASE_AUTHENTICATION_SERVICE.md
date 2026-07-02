# Firebase Authentication Service

## Purpose

The Firebase Authentication service verifies phone-authenticated Firebase ID tokens on the backend.

Firebase Phone Authentication owns the OTP challenge flow on the client side:

1. The client app starts Firebase Phone Auth.
2. Firebase sends and verifies the OTP.
3. Firebase returns an ID token to the client after successful phone verification.
4. The client sends that ID token to the NexaCore backend.
5. The backend verifies the token, confirms it came from phone sign-in, and extracts the verified phone number.

This service does not send SMS directly. SMS sending for non-Firebase workflows is handled by `messagingservice`; see `MESSAGING_SERVICE.md`.

## Package Structure

```text
com.nexacore.servicesmodule.firebaseauthservice
├── config
│   ├── FirebaseAdminConfig.java
│   └── FirebaseAuthenticationProperties.java
├── dto
│   ├── FirebasePhoneVerificationRequest.java
│   └── FirebasePhoneVerificationResponse.java
└── service
    ├── implementations
    │   └── FirebasePhoneVerificationServiceImpl.java
    └── interfaces
        └── FirebasePhoneVerificationService.java
```

## Component Responsibilities

### `FirebasePhoneVerificationService`

Service contract for verifying Firebase phone authentication tokens.

Methods:

- `verifyPhoneToken(String idToken)`: verifies a Firebase ID token.
- `verifyPhoneToken(FirebasePhoneVerificationRequest request)`: verifies an ID token and optionally checks it against an expected phone number.

### `FirebasePhoneVerificationServiceImpl`

Default Spring `@Service` implementation. It:

- validates the verification request.
- delegates Firebase ID token verification to `FirebaseAuth.verifyIdToken(...)`.
- confirms the token contains `phone_number`.
- confirms `firebase.sign_in_provider` is `phone`.
- optionally checks the token phone number against `expectedPhoneNumber`.
- returns verified Firebase phone identity data.

### `FirebaseAdminConfig`

Spring configuration that initializes the Firebase Admin SDK when `firebase.auth.enabled=true`.

It supports two credential modes:

- service-account JSON file through `firebase.auth.service-account-path`.
- Google Application Default Credentials when `firebase.auth.use-application-default-credentials=true`.

### `FirebaseAuthenticationProperties`

Spring configuration holder bound from `firebase.auth.*` properties.

### DTOs

`FirebasePhoneVerificationRequest`:

- `idToken`: Firebase ID token from the client.
- `expectedPhoneNumber`: optional phone number that must match the token claim.

`FirebasePhoneVerificationResponse`:

- `verified`
- `uid`
- `phoneNumber`
- `signInProvider`
- `issuer`
- `audience`
- `issuedAt`
- `expiresAt`
- `firebaseClaims`

## Technical Flow

```text
Client completes Firebase Phone Auth
  |
  v
Client receives Firebase ID token
  |
  v
Client sends token to backend
  |
  v
FirebasePhoneVerificationService.verifyPhoneToken(...)
  |
  v
Validate request
  |
  v
Use configured FirebaseApp / FirebaseAuth
  |
  v
FirebaseAuth.verifyIdToken(...)
  |
  v
Read phone_number and firebase.sign_in_provider claims
  |
  v
Ensure sign_in_provider == phone
  |
  v
Optionally compare expectedPhoneNumber
  |
  v
Return FirebasePhoneVerificationResponse
```

## Required Firebase Token Claims

The token must contain:

- `sub`: Firebase user UID.
- `aud`: Firebase project ID.
- `iss`: `https://securetoken.google.com/{projectId}`.
- `phone_number`: verified phone number.
- `firebase.sign_in_provider`: must be `phone`.

## Configuration

The Firebase Admin SDK dependency is declared in `backend/pom.xml`:

```xml
<dependency>
  <groupId>com.google.firebase</groupId>
  <artifactId>firebase-admin</artifactId>
  <version>9.9.0</version>
</dependency>
```

Configuration is defined in `backend/src/main/resources/application.properties`:

```properties
firebase.auth.enabled=${FIREBASE_AUTH_ENABLED:true}
firebase.auth.project-id=${FIREBASE_PROJECT_ID:kyc-a8e6d}
firebase.auth.service-account-path=${FIREBASE_SERVICE_ACCOUNT_PATH:src/main/java/com/nexacore/servicesmodule/firebaseauthservice/firebase-service-account.json}
firebase.auth.use-application-default-credentials=${FIREBASE_USE_APPLICATION_DEFAULT_CREDENTIALS:false}
firebase.auth.phone-sign-in-provider=${FIREBASE_PHONE_SIGN_IN_PROVIDER:phone}
```

Docker Compose passes the same environment variables to the backend service.

Minimum required runtime values:

```properties
FIREBASE_AUTH_ENABLED=true
FIREBASE_PROJECT_ID=kyc-a8e6d
FIREBASE_SERVICE_ACCOUNT_PATH=src/main/java/com/nexacore/servicesmodule/firebaseauthservice/firebase-service-account.json
```

For Docker Compose, the local file is mounted read-only into the backend container:

```text
./backend/src/main/java/com/nexacore/servicesmodule/firebaseauthservice/firebase-service-account.json
  -> /opt/nexacore/firebase/firebase-service-account.json
```

The container uses:

```properties
FIREBASE_SERVICE_ACCOUNT_PATH=/opt/nexacore/firebase/firebase-service-account.json
```

If `FIREBASE_SERVICE_ACCOUNT_PATH` is blank and `FIREBASE_USE_APPLICATION_DEFAULT_CREDENTIALS=true`, the SDK uses Google Application Default Credentials. For local development, this usually means setting:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/secure/path/firebase-service-account.json"
```

Do not commit the service-account JSON file.

## Usage Example

```java
@Service
public class ExamplePhoneVerificationWorkflow {

    private final FirebasePhoneVerificationService firebasePhoneVerificationService;

    public ExamplePhoneVerificationWorkflow(FirebasePhoneVerificationService firebasePhoneVerificationService) {
        this.firebasePhoneVerificationService = firebasePhoneVerificationService;
    }

    public FirebasePhoneVerificationResponse verify(String idToken, String expectedPhoneNumber) {
        return firebasePhoneVerificationService.verifyPhoneToken(
                FirebasePhoneVerificationRequest.builder()
                        .idToken(idToken)
                        .expectedPhoneNumber(expectedPhoneNumber)
                        .build()
        );
    }
}
```

## Error Behavior

The service throws:

- `IllegalStateException` when Firebase verification is disabled or required config is missing.
- `IllegalArgumentException` when the request or ID token is blank.
- `IllegalStateException` when Firebase Admin token verification fails, the token lacks a phone claim, was not issued by phone sign-in, or does not match the expected phone number.

## Integration Notes

- Use this service after the client has completed Firebase Phone Auth.
- Do not use this service to send OTP SMS. Firebase sends OTPs through the Firebase client SDK flow.
- Use `messagingservice` for system-generated SMS such as non-Firebase OTPs, transaction alerts, and operational notifications.
- For login flows, call this service first, then map the verified Firebase UID or phone number to a NexaCore user.
