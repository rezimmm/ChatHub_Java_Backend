package com.chathub.config;

import com.chathub.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIP(request);
        String uri = request.getRequestURI();

        Bucket bucket;
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register")) {
            bucket = rateLimitService.resolveAuthBucket(ip);
        } else if (uri.startsWith("/api/upload")) {
            bucket = rateLimitService.resolveUploadBucket(ip);
        } else if (uri.startsWith("/api/")) {
            bucket = rateLimitService.resolveGeneralBucket(ip);
        } else {
            return true; // Don't rate limit non-API paths (static assets, etc.)
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for IP: {} on URI: {}. Wait for {}s", ip, uri, waitForRefill);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
