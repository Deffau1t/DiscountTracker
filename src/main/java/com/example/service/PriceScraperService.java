package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceScraperService {
    private static final Logger log = LoggerFactory.getLogger(PriceScraperService.class);
    private static final Map<String, PriceScraper> SCRAPERS = new HashMap<>();
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\d+([.,]\\d{1,2})?");
    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    private EmailService emailService;

    public PriceScraperService() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // DNS
        SCRAPERS.put("dns", doc -> {
            try {
                driver.get(doc.location());
                Thread.sleep(2000);
                
                String[] selectors = {
                    ".product-buy__price",
                    ".product-buy__price span",
                    ".product-buy__price .price-block__final-price",
                    ".product-buy__price .price-block__price",
                    ".product-buy__price .price-block__price span",
                    ".product-buy__price .price-block__price .price",
                    ".product-buy__price .price-block__price .price-block__price-value",
                    ".product-buy__price .price-block__price .price-block__price-value span"
                };
                
                for (String selector : selectors) {
                    try {
                        WebElement priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                        if (priceElement != null) {
                            String price = priceElement.getText();
                            if (price != null && !price.isEmpty()) {
                                return extractPrice(price);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Не удалось найти цену по селектору {}: {}", selector, e.getMessage());
                    }
                }
                return null;
            } catch (Exception e) {
                log.error("Ошибка при поиске цены на DNS: {}", e.getMessage());
                return null;
            }
        });

        // Wildberries
        SCRAPERS.put("wildberries", doc -> {
            try {
                driver.get(doc.location());
                Thread.sleep(2000);
                
                String[] selectors = {
                    ".price-block__final-price",
                    ".price-block__price",
                    ".price-block__price span",
                    ".price-block__price .price",
                    ".price-block__price .price-block__price-value",
                    ".price-block__price .price-block__final-price",
                    ".price-block__price .price-block__final-price span",
                    ".price-block__price .price-block__price-value span"
                };
                
                for (String selector : selectors) {
                    try {
                        WebElement priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                        if (priceElement != null) {
                            String price = priceElement.getText();
                            if (price != null && !price.isEmpty()) {
                                return extractPrice(price);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Не удалось найти цену по селектору {}: {}", selector, e.getMessage());
                    }
                }
                return null;
            } catch (Exception e) {
                log.error("Ошибка при поиске цены на Wildberries: {}", e.getMessage());
                return null;
            }
        });

        // Ozon
        SCRAPERS.put("ozon", doc -> {
            try {
                driver.get(doc.location());
                Thread.sleep(3000);

                String[] selectors = {
                    "[data-widget=\"webPrice\"] span",
                    ".nl8 span",
                    ".tile-hover-target span[data-widget=\"price\"]",
                    ".tile-hover-target span[data-widget=\"webPrice\"]",
                    ".tile-hover-target span[data-widget=\"price\"] span",
                    ".tile-hover-target span[data-widget=\"webPrice\"] span",
                    ".tile-hover-target .tsBody500Medium",
                    ".tile-hover-target .tsHeadline500Medium",
                    ".tile-hover-target .tsBodyControl400Small",
                    ".tile-hover-target .tsBodyControl400Small span",
                    ".tile-hover-target .tsBodyControl400Small .tsBodyControl400Small",
                    ".tile-hover-target .tsBodyControl400Small .tsBodyControl400Small span",
                    ".tile-hover-target .tsBodyControl400Small .tsBodyControl400Small .tsBodyControl400Small",
                    ".tile-hover-target .tsBodyControl400Small .tsBodyControl400Small .tsBodyControl400Small span"
                };

                for (String selector : selectors) {
                    try {
                        WebElement priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                        if (priceElement != null) {
                            String priceText = priceElement.getText();
                            if (priceText != null && !priceText.isEmpty()) {
                                return extractPrice(priceText);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Не удалось найти цену по селектору {}: {}", selector, e.getMessage());
                    }
                }

                // Если не нашли цену, пробуем получить HTML страницы и поискать цену в нем
                String pageSource = driver.getPageSource();
                if (pageSource.contains("price")) {
                    log.info("Найдено упоминание цены в исходном коде страницы");
                    Pattern pricePattern = Pattern.compile("\"price\":\\s*\"?([0-9]+(?:[.,][0-9]{1,2})?)\"?");
                    Matcher matcher = pricePattern.matcher(pageSource);
                    if (matcher.find()) {
                        return extractPrice(matcher.group(1));
                    }
                }

                log.warn("Не удалось найти цену на странице Ozon");
                return null;
            } catch (Exception e) {
                log.error("Ошибка при поиске цены на Ozon: {}", e.getMessage());
                return null;
            }
        });
    }

    public BigDecimal scrapePrice(String url, String userEmail) throws IOException {
        String domain = extractDomain(url);
        PriceScraper scraper = SCRAPERS.get(domain);
        
        if (scraper == null) {
            log.warn("Нет поддержки для сайта: {}", domain);
            return null;
        }

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Cache-Control", "max-age=0")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .timeout(10000)
                .get();
            BigDecimal price = scraper.scrape(doc);
            if (price != null && userEmail != null && !userEmail.isEmpty()) {
                emailService.sendPriceNotification(url, price, userEmail);
            }
            return price;
        } catch (Exception e) {
            log.error("Ошибка при получении страницы {}: {}", url, e.getMessage());
            throw new IOException(e);
        }
    }

    private static String extractDomain(String url) {
        String domain = url.toLowerCase();
        if (domain.contains("ozon.ru")) return "ozon";
        if (domain.contains("wildberries.ru")) return "wildberries";
        if (domain.contains("citilink.ru")) return "citilink";
        if (domain.contains("dns-shop.ru")) return "dns";
        return null;
    }

    private static BigDecimal extractPrice(String text) {
        if (text == null) return null;
        
        Matcher matcher = PRICE_PATTERN.matcher(text.replace(" ", "").replace("₽", ""));
        if (matcher.find()) {
            String price = matcher.group().replace(",", ".");
            try {
                return new BigDecimal(price);
            } catch (NumberFormatException e) {
                log.error("Ошибка при парсинге цены: {}", price);
                return null;
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface PriceScraper {
        BigDecimal scrape(Document document);
    }
} 