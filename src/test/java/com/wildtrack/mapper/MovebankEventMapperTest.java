package com.wildtrack.mapper;

import com.wildtrack.client.dto.MovebankEventDto;
import com.wildtrack.model.MovebankEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class MovebankEventMapperTest {


    @Test
    void toEntity_mapsAllFieldsCorrectly(){

        MovebankEventMapper mapper = Mappers.getMapper(MovebankEventMapper.class);

            MovebankEventDto dto = new MovebankEventDto(
                    LocalDateTime.of(2011,1,11,1,1,11,111000000),
                    11.1111d,
                    -11.1111d,
                    "11111111",
                    "111111111"
            );

            MovebankEvent entity = mapper.toEntity(dto);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getTimestamp()).isEqualTo(dto.getTimestamp());
            assertThat(entity.getLocationLat()).isEqualTo(dto.getLocationLat());
            assertThat(entity.getLocationLong()).isEqualTo(dto.getLocationLong());
            assertThat(entity.getIndividualId()).isEqualTo(dto.getIndividualId());
            assertThat(entity.getTagId()).isEqualTo(dto.getTagId());
        }

    @Test
    void toDto_mapsAllFieldsCorrectly(){

        MovebankEventMapper mapper = Mappers.getMapper(MovebankEventMapper.class);

        MovebankEvent event = new MovebankEvent(
                LocalDateTime.of(2011,1,11,1,1,11,111000000),
                11.1111d,
                -11.1111d,
                "11111111",
                "111111111"
        );

        MovebankEventDto dto = mapper.toDto(event);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getTimestamp()).isEqualTo(event.getTimestamp());
        assertThat(dto.getLocationLat()).isEqualTo(event.getLocationLat());
        assertThat(dto.getLocationLong()).isEqualTo(event.getLocationLong());
        assertThat(dto.getIndividualId()).isEqualTo(event.getIndividualId());
        assertThat(dto.getTagId()).isEqualTo(event.getTagId());
    }
}
