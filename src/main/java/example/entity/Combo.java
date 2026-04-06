package example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Combos")
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "combo_name", nullable = false, columnDefinition = "nvarchar(255)")
    private String comboName;

    @Column(name = "description", columnDefinition = "nvarchar(MAX)")
    private String description; // Mô tả chi tiết

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "image_url", columnDefinition = "nvarchar(MAX)")
    private String imageUrl;

    @Column(name = "active")
    private Boolean active = true;

    // --- CONSTRUCTORS ---
    public Combo() {}

    // --- GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}