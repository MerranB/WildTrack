package com.wildtrack.service;

import com.wildtrack.demo.DemoService;
import com.wildtrack.dto.CoordinateDto;
import com.wildtrack.dto.GeoFenceDto;
import com.wildtrack.exception.VerificationCodeException;
import com.wildtrack.model.VerificationPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifiedActionServiceTest {

    private static final String EMAIL = "researcher@example.com";
    private static final String CODE = "123456";

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private GeoFenceService geoFenceService;

    @Mock
    private DemoService demoService;

    @InjectMocks
    private VerifiedActionService verifiedActionService;

    private GeoFenceDto sampleDto() {
        GeoFenceDto dto = new GeoFenceDto(
                "Test Fence",
                List.of(
                        new CoordinateDto(10.0, 20.0),
                        new CoordinateDto(10.0, 21.0),
                        new CoordinateDto(11.0, 21.0),
                        new CoordinateDto(10.0, 20.0)),
                EMAIL, "researcher1", 0);
        dto.setId(12L);
        return dto;
    }

    private void givenCodeAccepted(VerificationPurpose purpose) {
        when(emailVerificationService.consume(EMAIL, CODE, purpose, GeoFenceDto.class))
                .thenReturn(sampleDto());
    }

    private void givenCodeRejected(VerificationPurpose purpose) {
        when(emailVerificationService.consume(EMAIL, CODE, purpose, GeoFenceDto.class))
                .thenThrow(new VerificationCodeException("Incorrect code."));
    }

    @Test
    void completeGeoFence_createsTheFenceOnceTheCodeIsAccepted() {
        givenCodeAccepted(VerificationPurpose.GEO_FENCE);
        when(geoFenceService.create(any())).thenReturn(sampleDto());

        String message = verifiedActionService.completeGeoFence(EMAIL, CODE);

        verify(geoFenceService).create(any());
        assertThat(message).contains("Geo-fence 12");
    }

    @Test
    void completeGeoFence_consumesAgainstTheGeoFencePurpose() {
        givenCodeAccepted(VerificationPurpose.GEO_FENCE);
        when(geoFenceService.create(any())).thenReturn(sampleDto());

        verifiedActionService.completeGeoFence(EMAIL, CODE);

        verify(emailVerificationService)
                .consume(EMAIL, CODE, VerificationPurpose.GEO_FENCE, GeoFenceDto.class);
    }

    @Test
    void completeGeoFence_withARejectedCode_createsNothing() {
        givenCodeRejected(VerificationPurpose.GEO_FENCE);

        assertThatThrownBy(() -> verifiedActionService.completeGeoFence(EMAIL, CODE))
                .isInstanceOf(VerificationCodeException.class);

        verify(geoFenceService, never()).create(any());
    }

    @Test
    void completeDemo_runsTheDemoOnceTheCodeIsAccepted() {
        givenCodeAccepted(VerificationPurpose.DEMO);
        when(demoService.runDemo(any())).thenReturn("Your geofence has been successfully setup!");

        String message = verifiedActionService.completeDemo(EMAIL, CODE);

        verify(demoService).runDemo(any());
        assertThat(message).contains("Email confirmed").contains("successfully setup");
    }

    @Test
    void completeDemo_consumesAgainstTheDemoPurpose() {
        givenCodeAccepted(VerificationPurpose.DEMO);
        when(demoService.runDemo(any())).thenReturn("done");

        verifiedActionService.completeDemo(EMAIL, CODE);

        verify(emailVerificationService)
                .consume(EMAIL, CODE, VerificationPurpose.DEMO, GeoFenceDto.class);
    }

    /**
     * The demo is the one path that mails a stranger on demand, so a bad code has to stop it
     * before any message goes out.
     */
    @Test
    void completeDemo_withARejectedCode_sendsNothing() {
        givenCodeRejected(VerificationPurpose.DEMO);

        assertThatThrownBy(() -> verifiedActionService.completeDemo(EMAIL, CODE))
                .isInstanceOf(VerificationCodeException.class);

        verify(demoService, never()).runDemo(any());
    }
}
