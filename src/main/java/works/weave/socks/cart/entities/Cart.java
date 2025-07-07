package works.weave.socks.cart.entities;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document
public class Cart {

    @NotNull
    private String customerId;

    @Id
    private String id;

    @DBRef
    private List<Item> items = new ArrayList<>();

    public Cart(String customerId) {
        this.customerId = customerId;
    }

    public List<Item> contents() {
        return items;
    }

    public Cart add(Item item) {
        items.add(item);
        return this;
    }

    public Cart remove(Item item) {
        items.remove(item);
        return this;
    }
}
