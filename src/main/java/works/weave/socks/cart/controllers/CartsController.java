package works.weave.socks.cart.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import works.weave.socks.cart.cart.CartDAO;
import works.weave.socks.cart.cart.CartResource;
import works.weave.socks.cart.entities.Cart;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/carts")
public class CartsController {

    private static final Logger logger = LoggerFactory.getLogger(CartsController.class);

    private final CartDAO cartDAO;

    @Value("${http.timeout:5}")
    private long timeout;

    @Value("${simulate.latency:false}")
    private boolean simulateLatency;

    @Value("${simulate.cpu:false}")
    private boolean simulateCpu;

    @Value("${simulate.leak:false}")
    private boolean simulateLeak;

    @Value("${simulate.thread:false}")
    private boolean simulateThread;

    @Value("${simulate.deadlock:false}")
    private boolean simulateDeadlock;

    @Value("${simulate.error:false}")
    private boolean simulateError;

    private static int successfulOrderCount = 0;
    private static final List<byte[]> memoryLeakList = new CopyOnWriteArrayList<>();

    public CartsController(CartDAO cartDAO) {
        this.cartDAO = cartDAO;
    }

    @GetMapping(value = "/{customerId:^(?!metrics$).+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Cart get(@PathVariable String customerId) throws InterruptedException {
    simulateProblemsIfEnabled();
    successfulOrderCount++;
    return new CartResource(cartDAO, customerId).value().get();
}


    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void delete(@PathVariable String customerId) throws InterruptedException {
        simulateProblemsIfEnabled();
        new CartResource(cartDAO, customerId).destroy().run();
    }

    @GetMapping("/{customerId}/merge")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void mergeCarts(@PathVariable String customerId,
                           @RequestParam("sessionId") String sessionId) throws InterruptedException {
        simulateProblemsIfEnabled();
        logger.debug("Merge carts request received for ids: {} and {}", customerId, sessionId);
        var sessionCart = new CartResource(cartDAO, sessionId);
        var customerCart = new CartResource(cartDAO, customerId);
        customerCart.merge(sessionCart.value().get()).run();
        delete(sessionId);
    }

    private void simulateProblemsIfEnabled() throws InterruptedException {
        if (simulateLatency) {
            Thread.sleep(30000);
            logger.warn("🕒 Simulated latency (3s)");
        }

        if (simulateCpu) {
            for (int i = 0; i < 5_000_000; i++) {
                Math.log(Math.sqrt(i + 1));
            }
            logger.warn("🔥 Simulated CPU spike");
        }

        if (simulateLeak) {
            byte[] leak = new byte[10024 * 1024 * 1024];
            memoryLeakList.add(leak);
            logger.warn("💾 Simulated memory leak ({} blocks)", memoryLeakList.size());
        }

        if (simulateThread) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(100000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }).start();
            logger.warn("🧵 Simulated thread creation (WARNING: Thread leak!)");
        }

        if (simulateDeadlock) {
            final Object lock1 = new Object();
            final Object lock2 = new Object();

            Thread t1 = new Thread(() -> {
                synchronized (lock1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    synchronized (lock2) {
                        logger.warn("🔒 Thread 1 acquired both locks");
                    }
                }
            });

            Thread t2 = new Thread(() -> {
                synchronized (lock2) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    synchronized (lock1) {
                        logger.warn("🔒 Thread 2 acquired both locks");
                    }
                }
            });

            t1.start();
            t2.start();

            logger.warn("🔒 Simulated deadlock launched with 2 threads");
        }

        if (simulateError && successfulOrderCount >= 5) {
            logger.error("💥 Simulated error thrown (6th request or more)");
            throw new RuntimeException("Simulated error: 6th request failed");
        }
    }
}
