package com.wildtrack.controller;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.dto.ItemDto;
import com.wildtrack.service.ItemService;
import com.wildtrack.service.MovebankIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final MovebankIngestionService movebankIngestionService;


    @GetMapping
    public ResponseEntity<Page<ItemDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(itemService.findAll(pageable));
    }

    @GetMapping("/getData/{id}")
    public List<MovebankEventDto> getData(@PathVariable Long id) throws IOException {
        return movebankIngestionService.getData(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ItemDto> create(@Valid @RequestBody ItemDto dto) {
        ItemDto created = itemService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> update(@PathVariable Long id,
                                          @Valid @RequestBody ItemDto dto) {
        return ResponseEntity.ok(itemService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
