package dev.andymallcco.plutimi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PlutimiApplicationTests {

    @Test
    void contextLoads() {
    }

}
