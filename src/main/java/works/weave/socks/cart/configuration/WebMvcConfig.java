package works.weave.socks.cart.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;
import works.weave.socks.cart.middleware.HTTPMonitoringInterceptor;

@Configuration
public class WebMvcConfig {

    @Bean
    public HTTPMonitoringInterceptor httpMonitoringInterceptor() {
        return new HTTPMonitoringInterceptor();
    }

    @Bean
    public MappedInterceptor mappedInterceptor(HTTPMonitoringInterceptor interceptor) {
        return new MappedInterceptor(new String[]{"/**"}, interceptor);
    }
}
