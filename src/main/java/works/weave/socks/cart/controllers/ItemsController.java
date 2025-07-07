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

    public ItemsController(ItemDAO itemDAO, CartsController cartsController, CartDAO cartDAO) {
        this.itemDAO = itemDAO;
        this.cartsController = cartsController;
        this.cartDAO = cartDAO;
    }

    @GetMapping(value = "/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Item get(@PathVariable String customerId, @PathVariable String itemId)  throws InterruptedException {
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
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Item addToCart(@PathVariable String customerId, @RequestBody Item item)  throws InterruptedException {
        FoundItem foundItem = new FoundItem(() -> getItems(customerId), () -> item);
        successfulOrderCount++;
        simulateProblemsIfEnabled();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return item;
		});
        LOG.debug("Merging item in cart for user: {}, {}", customerId, item);
        itemResource.merge(item).run();
    }

    private void simulateProblemsIfEnabled() throws InterruptedException {
        if (simulateLatency) {
            Thread.sleep(3000);
            LOG.warn("🕒 Simulated latency (3s)");
        }

        if (simulateCpu) {
            for (int i = 0; i < 5_000_000; i++) {
                Math.log(Math.sqrt(i + 1));
            }
            LOG.warn("🔥 Simulated CPU spike");
        }

        if (simulateLeak) {
            byte[] leak = new byte[10 * 1024 * 1024];
            memoryLeakList.add(leak);
            LOG.warn("💾 Simulated memory leak ({} blocks)", memoryLeakList.size());
        }

        if (simulateThread) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }).start();
            LOG.warn("🧵 Simulated thread creation (WARNING: Thread leak!)");
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

        if (simulateError && successfulOrderCount >= 5) {
            LOG.error("💥 Simulated error thrown (6th request or more)");
            throw new RuntimeException("Simulated error: 6th request failed");
        }
    }
}
