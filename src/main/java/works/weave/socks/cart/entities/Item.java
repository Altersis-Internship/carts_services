package works.weave.socks.cart.entities;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    private String id;

    @NotNull(message = "Item Id must not be null")
    private String itemId;

    private int quantity;
    private float unitPrice;

    // Constructeur personnalisé pour initialiser par itemId seul
    public Item(String itemId) {
        this(null, itemId, 1, 0F);
    }

    // Constructeur basé sur un autre Item et un nouvel id
    public Item(Item item, String id) {
        this(id, item.itemId, item.quantity, item.unitPrice);
    }

    // Constructeur basé sur un autre Item avec nouvelle quantité
    public Item(Item item, int quantity) {
        this(item.getId(), item.itemId, quantity, item.unitPrice);
    }

    // equals uniquement sur itemId (conservation logique métier)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item item)) return false;
        return Objects.equals(itemId, item.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
}
