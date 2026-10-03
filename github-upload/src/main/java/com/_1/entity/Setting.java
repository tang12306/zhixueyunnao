package com._1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Setting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "setting_name", length = 255)
    private String name;

    @Column(name = "setting_description", length = 512)
    private String description;

    @Column(name = "setting_category", length = 100)
    private String category;

    @Column(name = "is_editable", nullable = false)
    private boolean isEditable = true; // Default to true

    public Setting(String key, String value, String name, String description, String category) {
        this.key = key;
        this.value = value;
        this.name = name;
        this.description = description;
        this.category = category;
        this.isEditable = true; // Default for this constructor
    }
} 