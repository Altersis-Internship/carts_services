package works.weave.socks.cart.item;

import works.weave.socks.cart.entities.Item;

import java.util.HashMap;
import java.util.Map;

public interface ItemDAO {
    Item save(Item item);

    void destroy(Item item);

    Item findOne(String id);

    class Fake implements ItemDAO {
        private final Map<String, Item> store = new HashMap<>();

        @Override
        public Item save(Item item) {
            store.put(item.getItemId(), item);
            return item;
        }

        @Override
        public void destroy(Item item) {
            store.remove(item.getItemId());
        }

        @Override
        public Item findOne(String id) {
            return store.values().stream()
                    .filter(i -> id.equals(i.getId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
