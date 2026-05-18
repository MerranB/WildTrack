package com.wildtrack.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailServiceImpl emailServiceImpl;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailServiceImpl, "sender", "test@example.com");
    }

    @Test
    void sendSimpleMail_returnsSuccess_whenEmailSent() {
        EmailDetail detail = new EmailDetail("recipient@example.com", "Test body", "Test subject", null);

        String result = emailServiceImpl.sendSimpleMail(detail);

        assertThat(result).isEqualTo("Mail Sent Successfully");
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleMail_returnsError_whenExceptionThrown() {
        EmailDetail detail = new EmailDetail("recipient@example.com", "Test body", "Test subject", null);
        doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));

        String result = emailServiceImpl.sendSimpleMail(detail);

        assertThat(result).isEqualTo("Error while sending mail");
    }
}
