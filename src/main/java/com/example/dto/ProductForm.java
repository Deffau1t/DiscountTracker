package com.example.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductForm {
        private String url;
        private String name;
        private String source;
        private String category;
        private BigDecimal threshold;
}