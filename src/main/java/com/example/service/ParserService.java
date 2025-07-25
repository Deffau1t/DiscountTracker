package com.example.service;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.repository.PriceHistoryRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Метод для парсинга цены в зависимости от источника.
     * @param product - объект продукта
     * @return BigDecimal - цена продукта
     */
    public BigDecimal parsePrice(Product product) {
        if (product.getSource().equalsIgnoreCase("my")) {
            // ✅ Парсинг с локального сайта, если источник "my"
            return fetchPriceFromLocal(product);
        } else {
            // ✅ Парсинг с внешнего сайта через Selenium
            return fetchPriceFromWeb(product);
        }
    }

    /**
     * Получение цены с локального сервера.
     * @return BigDecimal - цена продукта
     */
    private BigDecimal fetchPriceFromLocal(Product product) {
        try {
            // Извлечение ID из URL
            String productId = product.getUrl().substring(product.getUrl().lastIndexOf("/") + 1);

            // Формирование URL для запроса
            String apiUrl = "http://localhost:8081/products/api/" + productId;

            log.info("🌐 Запрос цены с локального сервера: {}", apiUrl);

            // Выполняем GET-запрос
            BigDecimal price = restTemplate.getForObject(apiUrl, BigDecimal.class);

            if (price != null) {
                log.info("💰 Цена успешно получена с локального сервера для ID {}: {}", productId, price);
                savePriceToHistory(product, price); // ✅ Теперь product передан в savePriceToHistory
                return price;
            } else {
                log.warn("⚠️ Цена не найдена на локальном сервере для ID: {}", productId);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при запросе к локальному серверу: {}", e.getMessage());
        }
        return null;
    }


    /**
     * Получение цены с внешнего сайта с помощью Selenium.
     * @param product - объект продукта
     * @return BigDecimal - цена продукта
     */
    private BigDecimal fetchPriceFromWeb(Product product) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        try {
            driver.get(product.getUrl());
            log.info("🌐 Открыта страница: {}", product.getUrl());

            WebElement priceElement = switch (product.getSource().toLowerCase()) {
                case "dns" -> driver.findElement(By.cssSelector(".product-buy__price"));
                case "citilink" -> {
                    try {
                        yield driver.findElement(By.cssSelector("[data-meta-name='PriceBlock'] span[data-meta-price]"));
                    } catch (Exception e) {
                        try {
                            yield driver.findElement(By.cssSelector(".ProductHeader__price-default_current-price"));
                        } catch (Exception e2) {
                            yield driver.findElement(By.cssSelector(".ProductPrice__price-current_current-price"));
                        }
                    }
                }
                case "ozon" -> {
                    try {
                        yield driver.findElement(By.cssSelector("[data-widget='webPrice'] span"));
                    } catch (Exception e) {
                        yield driver.findElement(By.cssSelector(".nl8 span"));
                    }
                }
                default -> throw new IllegalArgumentException("❌ Неизвестный источник: " + product.getSource());
            };

            String priceText = priceElement.getText().replaceAll("[^0-9]", "");
            if (!priceText.isEmpty()) {
                BigDecimal price = new BigDecimal(priceText);
                log.info("💰 Цена успешно получена для {}: {}", product.getName(), price);
                savePriceToHistory(product, price);
                return price;
            } else {
                log.warn("⚠️ Цена не найдена для продукта: {} на сайте {}", product.getName(), product.getSource());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при парсинге цены для {}: {}", product.getName(), e.getMessage());
        } finally {
            driver.quit();
        }
        return null;
    }

    /**
     * Сохранение истории цены продукта.
     * @param product - объект продукта
     * @param price - текущая цена
     */
    private void savePriceToHistory(Product product, BigDecimal price) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(price);
        priceHistory.setCheckedAt(LocalDateTime.now());
        priceHistoryRepository.save(priceHistory);

        log.info("💾 Цена успешно сохранена в историю для {}: {}", product.getName(), price);
    }
}
