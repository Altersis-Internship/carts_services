package works.weave.socks.cart.item;

import works.weave.socks.cart.cart.Resource;
import works.weave.socks.cart.entities.Item;

import java.util.function.Supplier;

public class ItemResource implements Resource<Item> {
    private final ItemDAO itemRepository;
    private final Supplier<Item> item;

    public ItemResource(ItemDAO itemRepository, Supplier<Item> item) {
        this.itemRepository = itemRepository;
        this.item = item;
    }

    @Override
    public Runnable destroy() {
        return () -> itemRepository.destroy(item.get());
    }

    @Override
    public Supplier<Item> create() {
        return () -> itemRepository.save(item.get());
    }

    @Override
    public Supplier<Item> value() {
        return item;
    }

    @Override
    public Runnable merge(Item toMerge) {
        return () -> {
            Item base = item.get();
            Item merged = new Item(base, toMerge.getQuantity());
            itemRepository.save(merged);
        };
    }
}
