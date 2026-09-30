package pe.edu.cacaoselva.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitFilterTest {
    @Test
    void limitaRegistroPorIpSinBloquearLecturasNiOtrosVisitantes() throws Exception {
        var filter = new AuthRateLimitFilter(new ObjectMapper().findAndRegisterModules());
        var attempts = new AtomicInteger();
        for (int i = 0; i < 6; i++) {
            var request = new MockHttpServletRequest("POST", "/auth/register"); request.setRemoteAddr("1.2.3.4");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> attempts.incrementAndGet());
            if (i == 5) { assertEquals(429, response.getStatus()); assertNotNull(response.getHeader("Retry-After")); }
        }
        assertEquals(5, attempts.get());
        var other = new MockHttpServletRequest("POST", "/auth/register"); other.setRemoteAddr("5.6.7.8");
        filter.doFilter(other, new MockHttpServletResponse(), (req, res) -> attempts.incrementAndGet());
        filter.doFilter(new MockHttpServletRequest("GET", "/lotes"), new MockHttpServletResponse(), (req, res) -> attempts.incrementAndGet());
        assertEquals(7, attempts.get());
    }
}
