package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecommendationDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productUrl;
    private String productCategory;
    private String productSource;
    private BigDecimal currentPrice;
    private BigDecimal score;
    private String algorithm;
    private Boolean isViewed;
}
