package com.example.demo.domain.enums;

/**
 * 파라미터 데이터 타입.
 * ERD에는 'Float', 'Int', 'Bool'로 표기되어 있으나
 * Java enum 컨벤션에 맞춰 대문자로 통일.
 * DB enum 정의도 'FLOAT', 'INT', 'BOOL'로 통일 권장.
 */
public enum DataType {
    FLOAT, INT, BOOL
}
