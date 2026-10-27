package vn.fitora.bootstrap.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.ProtectedController.class)
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousRequestsCannotAccessApplicationEndpoints() throws Exception {
        mvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticationAloneDoesNotBypassDenyByDefault() throws Exception {
        mvc.perform(get("/test/protected").with(user("test")))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "should not be exposed";
        }
    }
}
