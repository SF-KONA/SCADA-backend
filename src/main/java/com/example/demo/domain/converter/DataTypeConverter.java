package com.example.demo.domain.converter;

import com.example.demo.domain.enums.DataType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DataTypeConverter implements AttributeConverter<DataType, String> {

    @Override
    public String convertToDatabaseColumn(DataType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DataType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return DataType.valueOf(dbData.toUpperCase());
    }
}
