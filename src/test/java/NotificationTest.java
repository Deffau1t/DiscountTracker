import com.example.entity.Notification;
import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.repository.NotificationRepository;
import com.example.repository.PriceHistoryRepository;
import com.example.service.EmailService;
import com.example.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NotificationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNotificationSend() {
        // Подготовка данных
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        User user = new User();
        user.setEmail("test@example.com");

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUser(user);
        notification.setProduct(product);
        notification.setThreshold(new BigDecimal("1000.00"));

        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(new BigDecimal("900.00"));
        priceHistory.setCheckedAt(LocalDateTime.now());

        when(priceHistoryRepository.findByProductId(product.getId())).thenReturn(List.of(priceHistory));
        when(notificationRepository.findByNotifiedFalse()).thenReturn(List.of(notification));

        // Выполнение теста
        notificationService.checkAndSendNotifications();

        // Проверка
        verify(emailService, times(1)).sendNotification(user, product, priceHistory.getPrice());
        assertTrue(notification.isNotified());
    }
}
