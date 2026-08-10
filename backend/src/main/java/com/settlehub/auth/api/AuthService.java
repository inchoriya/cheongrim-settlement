package com.settlehub.auth.api;

import com.settlehub.auth.jwt.JwtTokenProvider;
import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserAccountRepository;
import com.settlehub.organization.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserAccount account = userAccountRepository.findByEmail(request.email())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> BusinessException.unauthorized("Invalid email or password"));

        AuthUser authUser = AuthUser.from(account);
        String token = jwtTokenProvider.createAccessToken(authUser);

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.expirationSeconds(),
                UserResponse.from(account)
        );
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Email already exists");
        }

        UserAccount account = switch (request.role()) {
            case ADMIN -> {
                if (request.agencyId() != null || request.merchantId() != null) {
                    throw BusinessException.badRequest("ADMIN must not have agencyId/merchantId");
                }
                yield UserAccount.admin(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name()
                );
            }
            case AGENCY -> {
                if (request.agencyId() == null || request.merchantId() != null) {
                    throw BusinessException.badRequest("AGENCY requires agencyId and no merchantId");
                }
                Agency agency = agencyRepository.findById(request.agencyId())
                        .orElseThrow(() -> BusinessException.notFound("Agency not found"));
                yield UserAccount.agencyUser(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name(),
                        agency
                );
            }
            case MERCHANT -> {
                if (request.merchantId() == null) {
                    throw BusinessException.badRequest("MERCHANT requires merchantId");
                }
                Merchant merchant = merchantRepository.findById(request.merchantId())
                        .orElseThrow(() -> BusinessException.notFound("Merchant not found"));
                yield UserAccount.merchantUser(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name(),
                        merchant
                );
            }
        };

        return UserResponse.from(userAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser authUser) {
        UserAccount account = userAccountRepository.findById(authUser.id())
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        return UserResponse.from(account);
    }
}
