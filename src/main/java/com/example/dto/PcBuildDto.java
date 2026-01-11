package com.example.dto;

import com.example.entity.PcBuild;
import com.example.entity.PcComponent;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO для сборки ПК
 */
@Data
public class PcBuildDto {
    private Long id;
    private String buildName;
    private String description;
    
    // Компоненты
    private ComponentDto cpu;
    private ComponentDto gpu;
    private ComponentDto motherboard;
    private ComponentDto ram;
    private ComponentDto storage;
    private ComponentDto psu;
    private ComponentDto pcCase;
    private ComponentDto cooler;
    
    private String buildStatus;
    private Boolean compatibilityChecked;
    private String compatibilityIssues;
    private BigDecimal totalPrice;
    private Boolean isPublic;
    
    /**
     * Конвертирует PcBuild в DTO
     */
    public static PcBuildDto fromEntity(PcBuild build) {
        PcBuildDto dto = new PcBuildDto();
        dto.setId(build.getId());
        dto.setBuildName(build.getBuildName());
        dto.setDescription(build.getDescription());
        dto.setBuildStatus(build.getBuildStatus() != null ? build.getBuildStatus().name() : null);
        dto.setCompatibilityChecked(build.getCompatibilityChecked());
        dto.setCompatibilityIssues(build.getCompatibilityIssues());
        dto.setTotalPrice(build.getTotalPrice());
        dto.setIsPublic(build.getIsPublic());
        
        if (build.getCpu() != null) {
            dto.setCpu(ComponentDto.fromEntity(build.getCpu()));
        }
        if (build.getGpu() != null) {
            dto.setGpu(ComponentDto.fromEntity(build.getGpu()));
        }
        if (build.getMotherboard() != null) {
            dto.setMotherboard(ComponentDto.fromEntity(build.getMotherboard()));
        }
        if (build.getRam() != null) {
            dto.setRam(ComponentDto.fromEntity(build.getRam()));
        }
        if (build.getStorage() != null) {
            dto.setStorage(ComponentDto.fromEntity(build.getStorage()));
        }
        if (build.getPsu() != null) {
            dto.setPsu(ComponentDto.fromEntity(build.getPsu()));
        }
        if (build.getPcCase() != null) {
            dto.setPcCase(ComponentDto.fromEntity(build.getPcCase()));
        }
        if (build.getCooler() != null) {
            dto.setCooler(ComponentDto.fromEntity(build.getCooler()));
        }
        
        return dto;
    }
    
    /**
     * DTO для компонента
     */
    @Data
    public static class ComponentDto {
        private Long id;
        private String componentType;
        private String name;
        private String manufacturer;
        private String model;
        private BigDecimal price;
        private String url;
        
        public static ComponentDto fromEntity(PcComponent component) {
            ComponentDto dto = new ComponentDto();
            dto.setId(component.getId());
            dto.setComponentType(component.getComponentType() != null ? component.getComponentType().name() : null);
            
            if (component.getProduct() != null) {
                dto.setName(component.getProduct().getName());
                dto.setPrice(component.getProduct().getCurrentPrice());
                dto.setUrl(component.getProduct().getUrl());
            }
            
            dto.setManufacturer(component.getManufacturer());
            dto.setModel(component.getModel());
            
            return dto;
        }
    }
}

