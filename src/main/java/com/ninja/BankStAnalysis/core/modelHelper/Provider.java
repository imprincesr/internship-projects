package com.ninja.BankStAnalysis.core.modelHelper;


public enum Provider {
    PERFIOS, SCOREME, FINBOX, ONEMONEY;

    public static Provider fromOrdinal(int ordinal) {
        for (Provider p : Provider.values()) {
            if (p.ordinal() == ordinal) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }
}