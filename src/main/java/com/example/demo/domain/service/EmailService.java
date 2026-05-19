package com.example.demo.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code, String purpose) {
        String subject = switch (purpose) {
            case "FIND_ID" -> "[SCADA] 아이디 찾기 인증번호";
            case "FIND_PW" -> "[SCADA] 비밀번호 재설정 인증번호";
            default        -> "[SCADA] 이메일 인증번호";
        };

        String text = """
                안녕하세요.
                
                요청하신 인증번호는 아래와 같습니다.
                
                인증번호: %s
                
                인증번호는 3분간 유효합니다.
                본인이 요청하지 않은 경우 이 메일을 무시해 주세요.
                """.formatted(code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendTemporaryPassword(String toEmail, String tempPassword) {
        String text = """
                안녕하세요.
                
                임시 비밀번호가 발급되었습니다.
                
                임시 비밀번호: %s
                
                로그인 후 반드시 비밀번호를 변경해 주세요.
                """.formatted(tempPassword);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[SCADA] 임시 비밀번호 발급");
        message.setText(text);
        mailSender.send(message);
    }
}