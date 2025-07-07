package works.weave.socks.cart.cart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.weave.socks.cart.entities.Cart;
import works.weave.socks.cart.entities.Item;

import java.util.List;
import java.util.function.Supplier;

public class CartContentsResource implements Contents<Item> {

    private static final Logger LOG = LoggerFactory.getLogger(CartContentsResource.class);

    private final CartDAO cartRepository;
    private final Supplier<Resource<Cart>> parent;

    public CartContentsResource(CartDAO cartRepository, Supplier<Resource<Cart>> parent) {
        this.cartRepository = cartRepository;
        this.parent = parent;
    }

    @Override
    public Supplier<List<Item>> contents() {
        return () -> parentCart().getItems();
    }

    @Override
    public Runnable add(Supplier<Item> item) {
        return () -> {
            LOG.debug("Adding for user: {}, {}", parentCart(), item.get());
            cartRepository.save(parentCart().add(item.get()));
        };
    }

    @Override
    public Runnable delete(Supplier<Item> item) {
        return () -> {
            LOG.debug("Deleting for user: {}, {}", parentCart(), item.get());
            cartRepository.save(parentCart().remove(item.get()));
        };
    }

    private Cart parentCart() {
        return parent.get().value().get();
    }
}
