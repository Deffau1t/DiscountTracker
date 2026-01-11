package com.example.dto;

import com.example.entity.PcComponent;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PcComponentDto {
    private Long id;
    private String componentType;
    private String manufacturer;
    private String model;
    private String name;
    private String url;
    private BigDecimal price;

    public static PcComponentDto fromEntity(PcComponent component) {
        PcComponentDto dto = new PcComponentDto();
        dto.setId(component.getId());
        dto.setComponentType(component.getComponentType() != null ? component.getComponentType().name() : null);
        dto.setManufacturer(component.getManufacturer());
        dto.setModel(component.getModel());
        
        if (component.getProduct() != null) {
            dto.setName(component.getProduct().getName());
            dto.setUrl(component.getProduct().getUrl());
            dto.setPrice(component.getProduct().getCurrentPrice());
        }
        
        return dto;
    }
}

