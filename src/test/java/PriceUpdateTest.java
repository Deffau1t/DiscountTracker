import com.example.entity.Product;
import com.example.service.ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PriceUpdateTest {

    @InjectMocks
    private ParserService parserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testParsePrice_Dns() {
        Product dnsProduct = new Product();
        dnsProduct.setUrl("https://www.dns-shop.ru/product/example");
        dnsProduct.setSource("dns");

        BigDecimal price = parserService.parsePrice(dnsProduct);

        assertNotNull(price, "Цена не должна быть null для DNS");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePrice_Citilink() {
        Product citilinkProduct = new Product();
        citilinkProduct.setUrl("https://www.citilink.ru/product/example");
        citilinkProduct.setSource("citilink");

        BigDecimal price = parserService.parsePrice(citilinkProduct);

        assertNotNull(price, "Цена не должна быть null для Citilink");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePrice_Ozon() {
        Product ozonProduct = new Product();
        ozonProduct.setUrl("https://www.ozon.ru/product/example");
        ozonProduct.setSource("ozon");

        BigDecimal price = parserService.parsePrice(ozonProduct);

        assertNotNull(price, "Цена не должна быть null для Ozon");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePrice_UnknownSource() {
        Product unknownProduct = new Product();
        unknownProduct.setUrl("https://example.com/product");
        unknownProduct.setSource("unknown");

        assertThrows(IllegalArgumentException.class, () -> parserService.parsePrice(unknownProduct));
    }
}
