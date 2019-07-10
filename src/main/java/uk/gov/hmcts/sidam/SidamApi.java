package uk.gov.hmcts.sidam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "SidamService")
public interface SidamApi {

    @RequestMapping(method = RequestMethod.GET, path = "/details")
    IdamUser getUser(@RequestHeader(value = "access_token") String accessToken);

    @RequestMapping(method = RequestMethod.POST, path = "/oauth2/authorize")
    AuthenticateUserResponse authorizeUser(
        @RequestHeader(value = "authorization") final String authorization,
        @RequestParam(value = "response_type", required = false, defaultValue = "token") final String response_type,
        @RequestParam(value = "client_id", required = false) final String client_id,
        @RequestParam(value = "redirect_uri", required = false) final String redirect_uri
    );
    // "{\"code\": , \"name\": \"tom\"}"
    @RequestMapping(value = "/oauth2/token", method = RequestMethod.POST)
    TokenExchangeResponse token(
        @RequestParam(value = "code") String code,
        @RequestParam(value = "grant_type") String grantType,
        /* REQUIRED, if the "redirect_uri" parameter was included in the authorization (WILL DO)  */
        @RequestParam(value = "redirect_uri") String redirectUri,
        /* REQUIRED, if the client is not authenticating with the authorization server (WILL DO) */
        @RequestParam(value = "client_id", required = false) String clientId,
        @RequestParam(value = "client_secret", required = false) String clientSecret
    );

    class AuthenticateUserResponse {
        @JsonProperty("code")
        private String code;

        public String getCode() {
            return code;
        }
    }

    class TokenExchangeResponse {

        @JsonProperty("access_token")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }
    }

    class IdamUser {
        @JsonProperty("id")
        private String id;

        @JsonProperty("roles")
        private List<String> roles;

        public String getId() {
            return id;
        }

        public List<String> getRoles() {
            return roles;
        }

    }
}
