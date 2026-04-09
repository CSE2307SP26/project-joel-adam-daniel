package bank;

public enum AccountType {
    CHECKING("Checking"),
    SAVINGS("Savings");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
