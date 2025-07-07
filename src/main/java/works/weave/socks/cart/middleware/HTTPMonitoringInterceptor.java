package works.weave.socks.cart.middleware;

import io.prometheus.client.Histogram;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Set;

public class HTTPMonitoringInterceptor implements HandlerInterceptor {

    private static final Histogram requestLatency = Histogram.build()
            .name("http_request_duration_seconds")
            .help("Request duration in seconds.")
            .labelNames("service", "method", "path", "status_code")
            .register();

    private static final String START_TIME_KEY = "startTime";

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private Set<PatternsRequestCondition> urlPatterns;

    @Value("${spring.application.name:carts}")
    private String serviceName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_KEY, System.nanoTime());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        Object attr = request.getAttribute(START_TIME_KEY);
        if (attr instanceof Long startTime) {
            long elapsed = System.nanoTime() - startTime;
            double seconds = elapsed / 1_000_000_000.0;
            String matchedUrl = getMatchingURLPattern(request);

            if (!matchedUrl.isEmpty()) {
                requestLatency.labels(
                        serviceName,
                        request.getMethod(),
                        matchedUrl,
                        Integer.toString(response.getStatus())
                ).observe(seconds);
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // No-op
    }

    private String getMatchingURLPattern(HttpServletRequest request) {
        for (PatternsRequestCondition pattern : getUrlPatterns()) {
            if (pattern != null &&
                    pattern.getMatchingCondition(request) != null &&
                    !"/error".equals(request.getServletPath())) {
                return pattern.getMatchingCondition(request)
                        .getPatterns().stream().findFirst().orElse("");
            }
        }
        return "";
    }

    private Set<PatternsRequestCondition> getUrlPatterns() {
        if (this.urlPatterns == null) {
            this.urlPatterns = new HashSet<>();
            requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, method) -> {
                PatternsRequestCondition pattern = mapping.getPatternsCondition();
                if (pattern != null) this.urlPatterns.add(pattern);
            });
        }
        return this.urlPatterns;
    }
}
