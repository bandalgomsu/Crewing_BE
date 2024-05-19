package com.crewing.auth.service;

import com.crewing.auth.dto.LoginDTO.LoginResponse;
import com.crewing.auth.dto.SignUpDTO.OauthSignUpRequest;
import com.crewing.auth.dto.SignUpDTO.TokenResponse;
import com.crewing.auth.jwt.service.JwtService;
import com.crewing.common.error.user.UserNotFoundException;
import com.crewing.external.OauthApi;
import com.crewing.external.OauthApi.GoogleOauth;
import com.crewing.external.OauthApi.KakaoOauth;
import com.crewing.external.OauthApi.NaverOauth;
import com.crewing.user.entity.Interest;
import com.crewing.user.entity.Role;
import com.crewing.user.entity.SocialType;
import com.crewing.user.entity.User;
import com.crewing.user.repository.InterestRepository;
import com.crewing.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthService {
    private final GoogleOauth googleOauth;
    private final NaverOauth naverOauth;
    private final KakaoOauth kakaoOauth;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final InterestRepository interestRepository;
    private final OauthApi oauthApi;

    public LoginResponse loginOauth(String oauthAccessToken, String social) {
        SocialType socialType = SocialType.valueOf(social);

        OAuthAttributes attributes = OAuthAttributes.of(socialType,
                oauthApi.getOauthUserInfo(oauthAccessToken, socialType));

        User user = getUser(attributes, socialType);

        String accessToken = jwtService.createAccessToken(user.getEmail());
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(user.getEmail(), refreshToken);

        TokenResponse tokenResponse = TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .build();

        if (user.getRole().equals(Role.GUEST)) {
            return LoginResponse.builder()
                    .tokenResponse(tokenResponse)
                    .needSignUp(true)
                    .build();
        }

        return LoginResponse.builder()
                .tokenResponse(tokenResponse)
                .build();
    }

    private User getUser(OAuthAttributes attributes, SocialType socialType) {
        User findUser = userRepository.findBySocialTypeAndSocialId(socialType, attributes.getOauth2UserInfo().getId())
                .orElse(null);

        if (findUser == null) {
            return saveUser(attributes, socialType);
        }

        return findUser;
    }

    private User saveUser(OAuthAttributes attributes, SocialType socialType) {
        User createdUser = attributes.toEntity(socialType, attributes.getOauth2UserInfo());
        return userRepository.save(createdUser);
    }

    private Map<String, Object> getAttributesInfo(String accessToken, SocialType socialType) {
        if (socialType.equals(SocialType.GOOGLE)) {
            googleOauth.getOauthUserInfo(accessToken);
        } else if (socialType.equals(SocialType.NAVER)) {
            naverOauth.getOauthUserInfo(accessToken);
        } else if (socialType.equals(SocialType.KAKAO)) {
            kakaoOauth.getOauthUserInfo(accessToken);
        }

        return null;
    }

    public void signUpOauth(OauthSignUpRequest request, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        List<Interest> interests = new ArrayList<>();
        request.getInterests().forEach(
                interest -> {
                    Interest save = Interest.builder().interest(interest).build();
                    interests.add(save);
                }
        );
        interestRepository.saveAll(interests);

        user.signUpOauth(request.getBirth(), request.getGender(), request.getName(), interests);

        userRepository.save(user);
    }
}