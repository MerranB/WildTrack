package com.wildtrack.repository;

import com.wildtrack.model.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findById_returnsEmpty_whenNotExists() {
        Optional<Item> found = itemRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllSavedItems() {
        itemRepository.save(new Item("Wolf", "Gray wolf"));
        itemRepository.save(new Item("Bear", "Grizzly bear"));

        List<Item> items = itemRepository.findAll();

        assertThat(items).hasSize(2);
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        assertThat(itemRepository.existsById(999L)).isFalse();
    }
}
