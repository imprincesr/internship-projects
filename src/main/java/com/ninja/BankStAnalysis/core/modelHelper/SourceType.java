package com.ninja.BankStAnalysis.core.modelHelper;


public enum SourceType {
    BATCH, REAL_TIME;

    public static SourceType fromOrdinal(int ordinal) {
        for (SourceType s : SourceType.values()) {
            if (s.ordinal() == ordinal) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }
}