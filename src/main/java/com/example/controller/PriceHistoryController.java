package com.example.controller;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;

    @GetMapping("/product/{id}/history")
    public String viewPriceHistory(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/watch-list";
        }

        List<PriceHistory> allHistory = priceHistoryRepository.findByProductId(id);
        allHistory.sort(Comparator.comparing(PriceHistory::getCheckedAt));

        // Фильтруем историю, оставляя только записи с изменением цены
        List<PriceHistory> filteredHistory = new ArrayList<>();
        BigDecimal lastPrice = null;

        for (PriceHistory history : allHistory) {
            if (lastPrice == null || lastPrice.compareTo(history.getPrice()) != 0) {
                filteredHistory.add(history);
                lastPrice = history.getPrice();
            }
        }

        model.addAttribute("product", product);
        model.addAttribute("historyList", filteredHistory);

        return "price-history";
    }
}
