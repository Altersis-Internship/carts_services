package works.weave.socks.cart.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import works.weave.socks.cart.entities.HealthCheck;

import java.util.*;

@RestController
public class HealthCheckController {

    private final MongoTemplate mongoTemplate;

    public HealthCheckController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(path = "/health")
    public @ResponseBody Map<String, List<HealthCheck>> getHealth() {
        Map<String, List<HealthCheck>> map = new HashMap<>();
        List<HealthCheck> healthChecks = new ArrayList<>();
        Date dateNow = Calendar.getInstance().getTime();

        HealthCheck app = new HealthCheck("carts", "OK", dateNow);
        HealthCheck database = new HealthCheck("carts-db", "OK", dateNow);

        try {
            mongoTemplate.executeCommand("{ buildInfo: 1 }");
        } catch (Exception e) {
            database.setStatus("err");
        }

        healthChecks.add(app);
        healthChecks.add(database);

        map.put("health", healthChecks);
        return map;
    }
}
