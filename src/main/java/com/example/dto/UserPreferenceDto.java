package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserPreferenceDto {
    private Long id;
    private String category;
    private BigDecimal weight;
}
