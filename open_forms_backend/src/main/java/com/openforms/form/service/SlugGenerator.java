package com.openforms.form.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 공개 폼 링크에 쓰는 8자 slug 를 생성합니다. 순차 id 를 노출하지 않도록 예측 불가능한 임의 문자열을 쓰며,
 * 충돌 시 재시도는 호출부(FormService)가 {@code findBySlug} 로 확인해 처리합니다.
 */
@Component
public class SlugGenerator {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    /** 소문자+숫자 8자 임의 문자열을 반환합니다. */
    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
