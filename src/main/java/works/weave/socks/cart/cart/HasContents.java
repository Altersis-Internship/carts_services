package works.weave.socks.cart.cart;

import java.util.function.Supplier;

@FunctionalInterface
public interface HasContents<T extends Contents<?>> {
    Supplier<T> contents();
}
