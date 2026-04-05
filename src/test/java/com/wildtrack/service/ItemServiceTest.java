package com.wildtrack.service;

import com.wildtrack.dto.ItemDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.ItemMapper;
import com.wildtrack.model.Item;
import com.wildtrack.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void findAll_returnsMappedDtos() {
        Item item = new Item("Wolf", "Gray wolf");
        ItemDto dto = ItemDto.of("Wolf", "Gray wolf");
        PageRequest pageable = PageRequest.of(0, 10);
        when(itemRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(item)));
        when(itemMapper.toDto(item)).thenReturn(dto);

        Page<ItemDto> result = itemService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Wolf");
    }

    @Test
    void findById_returnsDto_whenExists() {
        Item item = new Item("Wolf", "Gray wolf");
        ItemDto dto = ItemDto.of("Wolf", "Gray wolf");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(dto);

        ItemDto result = itemService.findById(1L);

        assertThat(result.name()).isEqualTo("Wolf");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item");
    }

    @Test
    void create_savesAndReturnsDto() {
        ItemDto inputDto = ItemDto.of("Wolf", "Gray wolf");
        Item entity = new Item("Wolf", "Gray wolf");
        ItemDto savedDto = ItemDto.of("Wolf", "Gray wolf");
        when(itemMapper.toEntity(inputDto)).thenReturn(entity);
        when(itemRepository.save(entity)).thenReturn(entity);
        when(itemMapper.toDto(entity)).thenReturn(savedDto);

        ItemDto result = itemService.create(inputDto);

        assertThat(result.name()).isEqualTo("Wolf");
        verify(itemRepository).save(entity);
    }

    @Test
    void update_updatesAndReturnsDto() {
        Item existing = new Item("OldName", "OldDesc");
        ItemDto updateDto = ItemDto.of("NewName", "NewDesc");
        ItemDto updatedDto = ItemDto.of("NewName", "NewDesc");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemMapper.toDto(existing)).thenReturn(updatedDto);

        ItemDto result = itemService.update(1L, updateDto);

        assertThat(result.name()).isEqualTo("NewName");
        verify(itemMapper).updateEntityFromDto(updateDto, existing);
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.update(99L, ItemDto.of("Name", "Desc")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesItem_whenExists() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        itemService.delete(1L);

        verify(itemRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(itemRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> itemService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
