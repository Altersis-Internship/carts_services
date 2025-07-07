package works.weave.socks.cart.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.weave.socks.cart.entities.Item;

import java.util.List;
import java.util.function.Supplier;

public class FoundItem implements Supplier<Item> {

    private static final Logger LOG = LoggerFactory.getLogger(FoundItem.class);

    private final Supplier<List<Item>> items;
    private final Supplier<Item> item;

    public FoundItem(Supplier<List<Item>> items, Supplier<Item> item) {
        this.items = items;
        this.item = item;
    }

    @Override
    public Item get() {
        return items.get().stream()
                .filter(item.get()::equals)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find item in cart"));
    }

    public boolean hasItem() {
        boolean present = items.get().stream()
                .anyMatch(item.get()::equals);

        LOG.debug("{} item: {}, in: {}", present ? "Found" : "Didn't find", item.get(), items.get());
        return present;
    }
}
