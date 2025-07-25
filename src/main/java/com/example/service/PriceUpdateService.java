package com.example.service;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ParserService parserService;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    @Scheduled(cron = "0 * * * * *")
    public void updatePrices() {
        log.info("Запуск обновления цен...");

        List<Product> products = productRepository.findAll();
        products.forEach(product -> executor.submit(() -> {
            BigDecimal newPrice = parserService.parsePrice(product);

            if (newPrice != null) {
                PriceHistory priceHistory = new PriceHistory();
                priceHistory.setProduct(product);
                priceHistory.setPrice(newPrice);
                priceHistory.setCheckedAt(LocalDateTime.now());
                priceHistoryRepository.save(priceHistory);

                log.info("Цена обновлена для продукта {}: {}", product.getName(), newPrice);
            }
        }));

        log.info("Обновление цен завершено.");
    }
}
