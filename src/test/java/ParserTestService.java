
import com.example.entity.Product;
import com.example.service.ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ParserTestService {

    @InjectMocks
    private ParserService parserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testParsePriceDns() {
        Product dnsProduct = new Product();
        dnsProduct.setUrl("https://www.dns-shop.ru/product/bcef83cb1e36ed20/videokarta-msi-geforce-rtx-4060-ti-ventus-2x-black-oc-geforce-rtx-4060-ti-ventus-2x-black-16g-oc/");
        dnsProduct.setSource("dns");

        BigDecimal price = parserService.parsePrice(dnsProduct);

        assertNotNull(price, "Цена не должна быть null для DNS");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePriceCitilink() {
        Product citilinkProduct = new Product();
        citilinkProduct.setUrl("https://www.citilink.ru/product/example");
        citilinkProduct.setSource("citilink");

        BigDecimal price = parserService.parsePrice(citilinkProduct);

        assertNotNull(price, "Цена не должна быть null для Citilink");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePriceOzon() {
        Product ozonProduct = new Product();
        ozonProduct.setUrl("https://www.ozon.ru/product/example");
        ozonProduct.setSource("ozon");

        BigDecimal price = parserService.parsePrice(ozonProduct);

        assertNotNull(price, "Цена не должна быть null для Ozon");
        assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Цена должна быть положительной");
    }

    @Test
    void testParsePriceUnknownSource() {
        Product unknownProduct = new Product();
        unknownProduct.setUrl("https://example.com/product");
        unknownProduct.setSource("unknown");

        assertThrows(IllegalArgumentException.class, () -> parserService.parsePrice(unknownProduct));
    }
}
