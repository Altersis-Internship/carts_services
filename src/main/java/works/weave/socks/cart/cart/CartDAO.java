package works.weave.socks.cart.cart;

import works.weave.socks.cart.entities.Cart;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CartDAO {
    void delete(Cart cart);
    Cart save(Cart cart);
    List<Cart> findByCustomerId(String customerId);

    class Fake implements CartDAO {
        private final Map<String, Cart> cartStore = new HashMap<>();

        @Override
        public void delete(Cart cart) {
            cartStore.remove(cart.getCustomerId());
        }

        @Override
        public Cart save(Cart cart) {
            cartStore.put(cart.getCustomerId(), cart);
            return cart;
        }

        @Override
        public List<Cart> findByCustomerId(String customerId) {
            Cart cart = cartStore.get(customerId);
            return cart != null ? List.of(cart) : Collections.emptyList();
        }
    }
}
