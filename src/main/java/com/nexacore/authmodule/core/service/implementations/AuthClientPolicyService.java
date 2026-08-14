package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.ResolvedAuthPolicy;
import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.exception.AuthPolicyException;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthClientPolicyService {

    private final AuthClientAuthPolicyRepository authPolicyRepository;

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public ResolvedAuthPolicy resolveRequiredPolicy(long tenantId, String clientCode) {
        if (tenantId <= 0 || !StringUtils.hasText(clientCode)) {
            throw AuthPolicyException.notConfigured();
        }

        final AuthClientAuthPolicy policy;
        try {
            policy = authPolicyRepository
                    .findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(tenantId, clientCode.trim())
                    .orElseThrow(AuthPolicyException::notConfigured);
        } catch (IncorrectResultSizeDataAccessException exception) {
            throw AuthPolicyException.integrityError();
        }

        if (policy.getLoginMethod() == null || policy.getLoginIdentifierType() == null
                || policy.getLoginMethod() == LoginMethod.MFA) {
            throw AuthPolicyException.integrityError();
        }

        return new ResolvedAuthPolicy(
                tenantId,
                policy.getClientCode(),
                policy.getLoginMethod(),
                policy.getLoginIdentifierType(),
                registrationCredentialModel(policy.getLoginMethod(), policy.getLoginIdentifierType()),
                policyVersion(policy)
        );
    }

    public void requireLoginMethod(ResolvedAuthPolicy policy, LoginMethod requestedMethod) {
        if (policy == null || requestedMethod == null || policy.loginMethod() != requestedMethod) {
            throw AuthPolicyException.methodNotAllowed();
        }
    }

    private RegistrationCredentialModel registrationCredentialModel(LoginMethod method,
                                                                     LoginIdentifierType identifier) {
        return switch (method) {
            case PASSWORD -> identifier == LoginIdentifierType.MOBILE
                    ? RegistrationCredentialModel.MOBILE_PASSWORD
                    : RegistrationCredentialModel.EMAIL_PASSWORD;
            case OTP -> RegistrationCredentialModel.MOBILE_OTP;
            case MAGIC_LINK -> RegistrationCredentialModel.MAGIC_LINK;
            case OAUTH -> RegistrationCredentialModel.SOCIAL_OAUTH;
            case SSO -> RegistrationCredentialModel.ENTERPRISE_SSO;
            case BIOMETRIC -> RegistrationCredentialModel.BIOMETRIC_ENROLLMENT;
            case PASSKEY -> RegistrationCredentialModel.PASSKEY_ENROLLMENT;
            case MFA -> throw AuthPolicyException.integrityError();
        };
    }

    private String policyVersion(AuthClientAuthPolicy policy) {
        LocalDateTime updatedAt = policy.getUpdatedAt();
        return policy.getId() + ":" + (updatedAt == null ? "0" : updatedAt.toString());
    }
}
