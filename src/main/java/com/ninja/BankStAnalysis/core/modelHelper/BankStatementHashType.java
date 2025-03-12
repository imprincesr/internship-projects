package com.ninja.BankStAnalysis.core.modelHelper;

public enum BankStatementHashType {
  BANK_TRANSACTION,
  ACCOUNT_XNS,
  EOD_BALANCE;

  public static BankStatementHashType fromOrdinal(int ordinal) {
    for (BankStatementHashType hT : BankStatementHashType.values()) {
      if (hT.ordinal() == ordinal) {
        return hT;
      }
    }
    throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
  }
}
