package works.weave.socks.cart.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheck {
    private String name;
    private String status;
    private Date date;
}
