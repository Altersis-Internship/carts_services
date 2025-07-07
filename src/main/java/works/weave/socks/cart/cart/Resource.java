package works.weave.socks.cart.cart;

import works.weave.socks.cart.entities.Cart;

import java.util.function.Supplier;

public interface Resource<T> {
    Runnable destroy();

    Supplier<T> create();

    Supplier<T> value();

    Runnable merge(T toMerge);

    class CartFake implements Resource<Cart> {
        private final String customerId;
        private Cart cart;

        public CartFake(String customerId) {
            this.customerId = customerId;
        }

        @Override
        public Runnable destroy() {
            return () -> cart = null;
        }

        @Override
        public Supplier<Cart> create() {
            return () -> {
                cart = new Cart(customerId);
                return cart;
            };
        }

        @Override
        public Supplier<Cart> value() {
            return () -> {
                if (cart == null) {
                    create().get();
                }
                return cart;
            };
        }

        @Override
        public Runnable merge(Cart toMerge) {
            return () -> {
                if (cart == null) {
                    create().get();
                }
                toMerge.getItems().forEach(item -> cart.add(item));
            };
        }
    }
}
