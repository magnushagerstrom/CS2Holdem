package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.Stamp;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a set of stamps as text, e.g. "HEADSHOT,SMOKED", and turns it back into a set.
 * Hibernate runs this automatically every time a stamps field is saved or loaded.
 */
@Converter
public class StampSetConverter implements AttributeConverter<Set<Stamp>, String> {

    @Override
    public String convertToDatabaseColumn(Set<Stamp> stamps) {
        if (stamps == null || stamps.isEmpty()) {
            return "";
        }
        return stamps.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Set<Stamp> convertToEntityAttribute(String value) {
        Set<Stamp> stamps = EnumSet.noneOf(Stamp.class);
        if (value == null || value.isBlank()) {
            return stamps;
        }
        for (String name : value.split(",")) {
            stamps.add(Stamp.valueOf(name.trim()));
        }
        return stamps;
    }
}
