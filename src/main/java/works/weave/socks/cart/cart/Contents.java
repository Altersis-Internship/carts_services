package works.weave.socks.cart.cart;

import works.weave.socks.cart.entities.Item;

import java.util.List;
import java.util.function.Supplier;

@FunctionalInterface
public interface Contents<T> {
    Supplier<List<T>> contents();

    default Runnable add(Supplier<Item> item) {
        throw new UnsupportedOperationException("Add operation not implemented");
    }

    default Runnable delete(Supplier<Item> item) {
        throw new UnsupportedOperationException("Delete operation not implemented");
    }
}
