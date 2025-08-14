package works.weave.socks.cart.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import works.weave.socks.cart.cart.CartDAO;
import works.weave.socks.cart.cart.CartResource;
import works.weave.socks.cart.entities.Item;
import works.weave.socks.cart.item.FoundItem;
import works.weave.socks.cart.item.ItemDAO;
import works.weave.socks.cart.item.ItemResource;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@RestController
@RequestMapping("/carts/{customerId}/items")
public class ItemsController {

    private static final Logger LOG = LoggerFactory.getLogger(ItemsController.class);

    private final ItemDAO itemDAO;
    private final CartsController cartsController;
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

    @Value("${simulate.leak.size:10240}") // en KB (10MB par défaut)
    private int leakSize;

    @Value("${simulate.leak.interval:1000}") // en ms
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

    public ItemsController(ItemDAO itemDAO, CartsController cartsController, CartDAO cartDAO) {
        this.itemDAO = itemDAO;
        this.cartsController = cartsController;
        this.cartDAO = cartDAO;
        
        // Démarrer les simulations au démarrage
        startMemoryLeak();
        simulateGcProblems();
        simulateCpuLoad();
        simulateThreadProblems();
        simulateBufferProblems();
    }

    @GetMapping(value = "/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Item get(@PathVariable String customerId, @PathVariable String itemId) throws InterruptedException {
        simulateProblemsIfEnabled();
        return new FoundItem(() -> getItems(customerId), () -> new Item(itemId)).get();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<Item> getItems(@PathVariable String customerId) {
        try {
            simulateProblemsIfEnabled();
            return cartsController.get(customerId).contents();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Thread interrupted while getting items", e);
            return List.of();
        }
    }
    private static int postRequestCount = 0;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Item addToCart(@PathVariable String customerId, @RequestBody Item item) throws InterruptedException {
        postRequestCount++;

        if (simulateError && postRequestCount >= 6) {
            LOG.error("💥 Simulated error on POST request #{}", postRequestCount);
            throw new RuntimeException("Simulated error: POST #" + postRequestCount + " failed");
        }

        simulateProblemsIfEnabled(); // <--- NE PAS compter ici

        FoundItem foundItem = new FoundItem(() -> getItems(customerId), () -> item);

        if (!foundItem.hasItem()) {
            Supplier<Item> newItem = new ItemResource(itemDAO, () -> item).create();
            LOG.debug("Did not find item. Creating item for user: {}, {}", customerId, newItem.get());
            new CartResource(cartDAO, customerId).contents().get().add(newItem).run();
            return item;
        } else {
            Item existingItem = foundItem.get();
            Item updatedItem = new Item(existingItem, existingItem.getQuantity() + 1);
            LOG.debug("Found item in cart. Incrementing for user: {}, {}", customerId, updatedItem);
            updateItem(customerId, updatedItem);
            return updatedItem;
        }
    }


    @GetMapping("/metrics")
    public int getSuccessfulOrderCount() {
        return successfulOrderCount;
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void removeItem(@PathVariable String customerId, @PathVariable String itemId) throws InterruptedException {
        FoundItem foundItem = new FoundItem(() -> getItems(customerId), () -> new Item(itemId));
        Item item = foundItem.get();
        simulateProblemsIfEnabled();
        
        LOG.debug("Removing item from cart: {}", item);
        new CartResource(cartDAO, customerId).contents().get().delete(() -> item).run();

        LOG.debug("Removing item from repository: {}", item);
        new ItemResource(itemDAO, () -> item).destroy().run();
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateItem(@PathVariable String customerId, @RequestBody Item item) throws InterruptedException {
        simulateProblemsIfEnabled();
        ItemResource itemResource = new ItemResource(itemDAO, () -> {
            try {
                return get(customerId, item.getItemId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Thread interrupted while getting item", e);
                return item;
            }
        });
        
        LOG.debug("Merging item in cart for user: {}, {}", customerId, item);
        itemResource.merge(item).run();
    }

    @GetMapping("/simulateError")
    public void simulateHttpError() {
        if (simulateError) {
            LOG.error("💥 Simulating HTTP error");
            throw new RuntimeException("Simulated HTTP error");
        }
    }

    private void simulateProblemsIfEnabled() throws InterruptedException {
       
        if (simulateLatency) {
            Thread.sleep(timeout);
            LOG.warn("🕒 Simulated latency ({}ms)", timeout);
        }

        if (simulateCpu) {
            for (int i = 0; i < 5_000_000; i++) {
                Math.log(Math.sqrt(i + 1));
            }
            LOG.warn("🔥 Simulated CPU spike");
        }

        if (simulateLeak) {
            byte[] leak = new byte[leakSize * 1024];
            memoryLeakList.add(leak);
            LOG.warn("💾 Simulated memory leak ({} blocks, total: {}MB)", 
                memoryLeakList.size(), memoryLeakList.size() * leakSize / 1024);
        }

    }

    private void startMemoryLeak() {
        if (simulateLeak) {
            new Thread(() -> {
                while (true) {
                    byte[] leak = new byte[leakSize * 1024];
                    memoryLeakList.add(leak);
                    LOG.warn("💾 Added {}KB to memory leak (total: {}MB)", 
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
                    System.gc();
                    LOG.warn("🗑️ Forced garbage collection");
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
            LOG.warn("🔥 Started CPU load on {} threads", threads);
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
            LOG.warn("🧵 Created {} blocked threads", threadCount);
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
                        LOG.warn("🔒 Thread 1 acquired both locks");
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
                        LOG.warn("🔒 Thread 2 acquired both locks");
                    }
                }
            });

            t1.start();
            t2.start();

            LOG.warn("🔒 Simulated deadlock launched with 2 threads");
        }
    }

    private void simulateBufferProblems() {
        if (simulateBuffer) {
            new Thread(() -> {
                while (true) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
                    directBuffers.add(buffer);
                    LOG.warn("📦 Allocated direct buffer (total: {}MB)", 
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