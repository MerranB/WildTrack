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
    void save_persistsItem() {
        Item item = new Item("Wolf", "Gray wolf tracked in Yellowstone");

        Item saved = itemRepository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Wolf");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_returnsItem_whenExists() {
        Item saved = itemRepository.save(new Item("Bear", "Grizzly bear"));

        Optional<Item> found = itemRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bear");
    }

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
    void save_updatesExistingItem() {
        Item saved = itemRepository.save(new Item("Wolf", "Original description"));
        saved.setDescription("Updated description");

        Item updated = itemRepository.save(saved);

        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void deleteById_removesItem() {
        Item saved = itemRepository.save(new Item("Wolf", "To be deleted"));

        itemRepository.deleteById(saved.getId());

        assertThat(itemRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        Item saved = itemRepository.save(new Item("Wolf", "Gray wolf"));

        assertThat(itemRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        assertThat(itemRepository.existsById(999L)).isFalse();
    }
}
