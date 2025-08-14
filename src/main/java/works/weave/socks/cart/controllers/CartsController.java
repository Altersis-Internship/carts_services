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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/carts")
public class CartsController {

    private static final Logger logger = LoggerFactory.getLogger(CartsController.class);

    private final CartDAO cartDAO;

    // Configuration des simulations
    @Value("${http.timeout:5000}")
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

    @Value("${simulate.gc:false}")
    private boolean simulateGc;

    @Value("${simulate.buffer:false}")
    private boolean simulateBuffer;

    @Value("${simulate.leak.size:1024}")
    private int leakSize = 1024; // Hardcode temporarily

    @Value("${simulate.leak.interval:5000}") // en ms
    private int leakInterval;

    @Value("${simulate.gc.interval:30000}") // en ms
    private int gcInterval;

    @Value("${simulate.thread.count:50}")
    private int threadCount;

    @Value("${simulate.buffer.size:1048576}") // 1MB
    private int bufferSize;

    private static int successfulOrderCount = 0;
    private static final List<byte[]> memoryLeakList = new CopyOnWriteArrayList<>();
    private static final List<ByteBuffer> directBuffers = new CopyOnWriteArrayList<>();

    public CartsController(CartDAO cartDAO) {
        this.cartDAO = cartDAO;
        
        // Démarrer les simulations au démarrage
        startMemoryLeak();
        simulateGcProblems();
        simulateCpuLoad();
        simulateThreadProblems();
        simulateBufferProblems();
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
         successfulOrderCount++;
        if (simulateLatency) {
            Thread.sleep(timeout);
            logger.warn("🕒 Simulated latency ({}ms)", timeout);
        }

        if (simulateCpu) {
            for (int i = 0; i < 5_000_00000; i++) {
                Math.log(Math.sqrt(i + 1));
            }
            logger.warn("🔥 Simulated CPU spike");
        }

        if (simulateLeak) {
            byte[] leak = new byte[leakSize * 1024 * 1024 ];
            memoryLeakList.add(leak);
            logger.warn("💾 Simulated memory leak ({} blocks, total: {}MB)", 
                memoryLeakList.size(), memoryLeakList.size() * leakSize / 1024);
        }

        if (simulateError && successfulOrderCount >= 5) {
            logger.error("💥 Simulated error thrown (6th request or more)");
            throw new RuntimeException("Simulated error: 6th request failed");
        }
    }

    private void startMemoryLeak() {
        if (simulateLeak) {
            new Thread(() -> {
                while (true) {
                    byte[] leak = new byte[leakSize * 1024 * 1024];
                    memoryLeakList.add(leak);
                    logger.warn("💾 Added {}KB to memory leak (total: {}MB)", 
                        leakSize, memoryLeakList.size() * leakSize / 1024);
                    try {
                        Thread.sleep(leakInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }

    private void simulateGcProblems() {
        if (simulateGc) {
            new Thread(() -> {
                while (true) {
                    System.gc(); // Force GC fréquemment
                    logger.warn("🗑️ Forced garbage collection");
                    try {
                        Thread.sleep(gcInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }

    private void simulateCpuLoad() {
        if (simulateCpu) {
            int threads = Runtime.getRuntime().availableProcessors();
            for (int i = 0; i < threads; i++) {
                new Thread(() -> {
                    while (true) {
                        long start = System.currentTimeMillis();
                        while (System.currentTimeMillis() - start < 1000) {
                            Math.pow(Math.PI, Math.PI);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }).start();
            }
            logger.warn("🔥 Started CPU load on {} threads", threads);
        }
    }

    private void simulateThreadProblems() {
        if (simulateThread) {
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    synchronized (this) {
                        try {
                            wait(); // Threads bloqués indéfiniment
                        } catch (InterruptedException ignored) {
                        }
                    }
                }).start();
            }
            logger.warn("🧵 Created {} blocked threads", threadCount);
        }
      if (simulateDeadlock) {
    final Object lock1 = new Object();
    final Object lock2 = new Object();
    final Object lock3 = new Object();
    final Object lock4 = new Object();
    final Object lock5 = new Object();

    Thread t1 = new Thread(() -> {
        synchronized (lock1) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            synchronized (lock2) {
                logger.warn("🔒 Thread 1 acquired lock1 and lock2");
            }
        }
    });

    Thread t2 = new Thread(() -> {
        synchronized (lock2) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            synchronized (lock3) {
                logger.warn("🔒 Thread 2 acquired lock2 and lock3");
            }
        }
    });

    Thread t3 = new Thread(() -> {
        synchronized (lock3) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            synchronized (lock4) {
                logger.warn("🔒 Thread 3 acquired lock3 and lock4");
            }
        }
    });

    Thread t4 = new Thread(() -> {
        synchronized (lock4) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            synchronized (lock5) {
                logger.warn("🔒 Thread 4 acquired lock4 and lock5");
            }
        }
    });

    Thread t5 = new Thread(() -> {
        synchronized (lock5) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            synchronized (lock1) {
                logger.warn("🔒 Thread 5 acquired lock5 and lock1");
            }
        }
    });

    t1.start();
    t2.start();
    t3.start();
    t4.start();
    t5.start();

    logger.warn("⚠️ Deadlock circulaire simulé avec 5 threads");
}}

    private void simulateBufferProblems() {
        if (simulateBuffer) {
            new Thread(() -> {
                while (true) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
                    directBuffers.add(buffer);
                    logger.warn("📦 Allocated direct buffer (total: {}MB)", 
                        directBuffers.size() * bufferSize / 1024 / 1024);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}