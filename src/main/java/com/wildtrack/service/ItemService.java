package com.wildtrack.service;

import com.wildtrack.dto.ItemDto;
import com.wildtrack.exception.ResourceNotFoundException;
import com.wildtrack.mapper.ItemMapper;
import com.wildtrack.model.Item;
import com.wildtrack.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;


    public Page<ItemDto> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable)
                .map(itemMapper::toDto);
    }

    public ItemDto findById(Long id) {
        return itemRepository.findById(id)
                .map(itemMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Item", id));
    }

    @Transactional
    public ItemDto create(ItemDto dto) {
        Item saved = itemRepository.save(itemMapper.toEntity(dto));
        return itemMapper.toDto(saved);
    }

    @Transactional
    public ItemDto update(Long id, ItemDto dto) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", id));
        itemMapper.updateEntityFromDto(dto, existing);
        return itemMapper.toDto(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item", id);
        }
        itemRepository.deleteById(id);
    }
}
