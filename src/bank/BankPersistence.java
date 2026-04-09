package bank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BankPersistence {

    public static final String DEFAULT_FILENAME = "bank-data.txt";

    static final String HEADER_V1 = "BANK_PERSIST_V1";
    static final String HEADER_V2 = "BANK_PERSIST_V2";
    static final String HEADER_V3 = "BANK_PERSIST_V3";
    /** V3 ACC line plus {@code |pin|fail|lock|minBalance} at end. */
    static final String HEADER_V4 = "BANK_PERSIST_V4";

    private BankPersistence() {}

    public static Path defaultPath() {
        return Path.of(DEFAULT_FILENAME);
    }

    public static void save(Bank bank, String activeAccountId, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(HEADER_V4).append('\n');
        sb.append("ACTIVE|").append(escapeField(activeAccountId)).append('\n');

        for (Account acc : bank.getAllAccounts()) {
            PinLogin p = acc.pinLoginForPersistence();
            sb.append("ACC|")
                    .append(escapeField(acc.getAccountNumber()))
                    .append('|')
                    .append(formatDouble(acc.getBalance()))
                    .append('|')
                    .append(acc.getAccountType().name());

            if (acc.getAccountType() == AccountType.SAVINGS) {
                sb.append('|')
                        .append(escapeField(acc.getSavingsPeriodKeyForPersistence()))
                        .append('|')
                        .append(acc.getSavingsWithdrawalsThisPeriodForPersistence());
            }

            sb.append('|').append(acc.isFrozen() ? "1" : "0");
            sb.append('|').append(p.pinCodeForPersistence());
            sb.append('|').append(p.failedAttemptsForPersistence());
            sb.append('|').append(p.lockedForPersistence() ? "1" : "0");
            sb.append('|').append(formatDouble(acc.getMinimumBalanceThreshold())).append('\n');

            for (Transaction t : acc.getTransactionHistory()) {
                sb.append("TXN|")
                        .append(t.getType().name())
                        .append('|')
                        .append(formatDouble(t.getAmount()))
                        .append('|')
                        .append(t.getWhenMs())
                        .append('|')
                        .append(escapeDetail(t.getDetail()))
                        .append('\n');
            }

            sb.append("ENDACC\n");
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    public static Optional<BankSnapshot> tryLoad(Path path) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(load(path));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load bank data, starting fresh: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static BankSnapshot load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Missing or invalid header.");
        }

        String header = lines.get(0).trim();
        boolean formatV4 = HEADER_V4.equals(header);
        boolean formatV3 = HEADER_V3.equals(header);
        boolean formatV2 = HEADER_V2.equals(header);
        boolean formatV1 = HEADER_V1.equals(header);
        if (!formatV4 && !formatV3 && !formatV2 && !formatV1) {
            throw new IllegalArgumentException("Missing or invalid header.");
        }

        int i = 1;
        if (i >= lines.size()) {
            throw new IllegalArgumentException("Missing ACTIVE line.");
        }

        String activeLine = lines.get(i++).trim();
        if (!activeLine.startsWith("ACTIVE|")) {
            throw new IllegalArgumentException("Expected ACTIVE line.");
        }

        String activeAccountId = unescapeField(activeLine.substring("ACTIVE|".length()));
        Bank bank = new Bank();

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            if (line.isEmpty()) {
                i++;
                continue;
            }

            if (!line.startsWith("ACC|")) {
                throw new IllegalArgumentException("Expected ACC line, got: " + line);
            }

            String accPayload = line.substring("ACC|".length());
            AccLineMeta meta;
            if (formatV4) {
                meta = parseAccountLineV4(accPayload);
            } else if (formatV3) {
                meta = parseAccountLineV3(accPayload);
            } else if (formatV2) {
                meta = parseAccountLineV2(accPayload);
            } else {
                meta = parseAccountLineV1(accPayload);
            }
            i++;

            List<Transaction> txns = new ArrayList<>();

            while (i < lines.size()) {
                String txLine = lines.get(i++);

                if (txLine.trim().equals("ENDACC")) {
                    break;
                }

                txns.add(parseTxnLine(txLine));
            }

            bank.addAccount(
                    Account.fromPersisted(
                            meta.accountId,
                            meta.balance,
                            txns,
                            meta.accountType,
                            meta.savingsPeriodKey,
                            meta.savingsWithdrawalsThisPeriod,
                            meta.frozen,
                            meta.persistedPin,
                            meta.pinFailedAttempts,
                            meta.pinLocked,
                            meta.minimumBalanceThreshold));
        }

        if (bank.getAccountCount() == 0) {
            throw new IllegalArgumentException("No accounts in file.");
        }

        if (bank.getAccount(activeAccountId) == null) {
            activeAccountId = bank.getAllAccounts().iterator().next().getAccountNumber();
        }

        return new BankSnapshot(bank, activeAccountId);
    }

    private static final class AccLineMeta {
        final String accountId;
        final double balance;
        final AccountType accountType;
        final String savingsPeriodKey;
        final int savingsWithdrawalsThisPeriod;
        final boolean frozen;
        final int persistedPin;
        final int pinFailedAttempts;
        final boolean pinLocked;
        final double minimumBalanceThreshold;

        AccLineMeta(
                String accountId,
                double balance,
                AccountType accountType,
                String savingsPeriodKey,
                int savingsWithdrawalsThisPeriod,
                boolean frozen,
                int persistedPin,
                int pinFailedAttempts,
                boolean pinLocked,
                double minimumBalanceThreshold) {
            this.accountId = accountId;
            this.balance = balance;
            this.accountType = accountType;
            this.savingsPeriodKey = savingsPeriodKey;
            this.savingsWithdrawalsThisPeriod = savingsWithdrawalsThisPeriod;
            this.frozen = frozen;
            this.persistedPin = persistedPin;
            this.pinFailedAttempts = pinFailedAttempts;
            this.pinLocked = pinLocked;
            this.minimumBalanceThreshold = minimumBalanceThreshold;
        }
    }

    private static AccLineMeta parseAccountLineV4(String accPayload) {
        int p = accPayload.lastIndexOf('|');
        double minBal = Double.parseDouble(accPayload.substring(p + 1));
        String rest = accPayload.substring(0, p);

        p = rest.lastIndexOf('|');
        String lockTok = rest.substring(p + 1);
        if (!"0".equals(lockTok) && !"1".equals(lockTok)) {
            throw new IllegalArgumentException("Invalid PIN lock flag.");
        }
        boolean pinLocked = "1".equals(lockTok);
        rest = rest.substring(0, p);

        p = rest.lastIndexOf('|');
        int pinFail = Integer.parseInt(rest.substring(p + 1));
        rest = rest.substring(0, p);

        p = rest.lastIndexOf('|');
        int pin = Integer.parseInt(rest.substring(p + 1));
        rest = rest.substring(0, p);

        AccLineMeta base = parseAccountLineV3(rest);
        return new AccLineMeta(
                base.accountId,
                base.balance,
                base.accountType,
                base.savingsPeriodKey,
                base.savingsWithdrawalsThisPeriod,
                base.frozen,
                pin,
                pinFail,
                pinLocked,
                minBal);
    }

    private static AccLineMeta parseAccountLineV1(String accPayload) {
        int balSep = accPayload.lastIndexOf('|');
        if (balSep < 0) {
            throw new IllegalArgumentException("Invalid ACC line.");
        }

        String accountId = unescapeField(accPayload.substring(0, balSep));
        double balance = Double.parseDouble(accPayload.substring(balSep + 1));
        String ym = YearMonth.now(ZoneId.systemDefault()).toString();
        return new AccLineMeta(
                accountId, balance, AccountType.CHECKING, ym, 0, false,
                Account.DEFAULT_TEST_PIN, 0, false, Account.DEFAULT_MINIMUM_BALANCE_THRESHOLD);
    }

    private static AccLineMeta parseAccountLineV2(String accPayload) {
        return parseAccountLineV2Core(accPayload);
    }

    private static AccLineMeta parseAccountLineV3(String accPayload) {
        int pFrozen = accPayload.lastIndexOf('|');
        if (pFrozen < 0) {
            throw new IllegalArgumentException("Invalid ACC line.");
        }
        String flag = accPayload.substring(pFrozen + 1);
        if (!"0".equals(flag) && !"1".equals(flag)) {
            throw new IllegalArgumentException("Invalid frozen flag in ACC line.");
        }
        boolean frozen = "1".equals(flag);
        String corePayload = accPayload.substring(0, pFrozen);
        AccLineMeta core = parseAccountLineV2Core(corePayload);
        return new AccLineMeta(
                core.accountId,
                core.balance,
                core.accountType,
                core.savingsPeriodKey,
                core.savingsWithdrawalsThisPeriod,
                frozen,
                Account.DEFAULT_TEST_PIN,
                0,
                false,
                Account.DEFAULT_MINIMUM_BALANCE_THRESHOLD);
    }

    private static AccLineMeta parseAccountLineV2Core(String accPayload) {
        int pRight = accPayload.lastIndexOf('|');
        if (pRight < 0) {
            throw new IllegalArgumentException("Invalid ACC line.");
        }

        String rightToken = accPayload.substring(pRight + 1);

        if (AccountType.CHECKING.name().equals(rightToken)) {
            int pBal = accPayload.lastIndexOf('|', pRight - 1);
            if (pBal < 0) {
                throw new IllegalArgumentException("Invalid ACC line.");
            }
            double balance = Double.parseDouble(accPayload.substring(pBal + 1, pRight));
            String accountId = unescapeField(accPayload.substring(0, pBal));
            String ym = YearMonth.now(ZoneId.systemDefault()).toString();
            return new AccLineMeta(
                    accountId, balance, AccountType.CHECKING, ym, 0, false,
                    Account.DEFAULT_TEST_PIN, 0, false, Account.DEFAULT_MINIMUM_BALANCE_THRESHOLD);
        }

        int pCount = pRight;
        int pPeriod = accPayload.lastIndexOf('|', pCount - 1);
        int pSav = accPayload.lastIndexOf('|', pPeriod - 1);
        int pBal = accPayload.lastIndexOf('|', pSav - 1);
        if (pBal < 0) {
            throw new IllegalArgumentException("Invalid ACC line.");
        }

        int withdrawals = Integer.parseInt(accPayload.substring(pCount + 1));
        String periodKey = unescapeField(accPayload.substring(pPeriod + 1, pCount));
        String typeStr = accPayload.substring(pSav + 1, pPeriod);
        if (!AccountType.SAVINGS.name().equals(typeStr)) {
            throw new IllegalArgumentException("Unknown account type in ACC line: " + typeStr);
        }
        double balance = Double.parseDouble(accPayload.substring(pBal + 1, pSav));
        String accountId = unescapeField(accPayload.substring(0, pBal));
        return new AccLineMeta(
                accountId, balance, AccountType.SAVINGS, periodKey, withdrawals, false,
                Account.DEFAULT_TEST_PIN, 0, false, Account.DEFAULT_MINIMUM_BALANCE_THRESHOLD);
    }

    private static Transaction parseTxnLine(String line) {
        if (!line.startsWith("TXN|")) {
            throw new IllegalArgumentException("Invalid TXN line.");
        }

        String payload = line.substring("TXN|".length());

        int p1 = payload.indexOf('|');
        int p2 = payload.indexOf('|', p1 + 1);
        int p3 = payload.indexOf('|', p2 + 1);

        if (p1 < 0 || p2 < 0 || p3 < 0) {
            throw new IllegalArgumentException("Malformed TXN line.");
        }

        Transaction.Type type = Transaction.Type.valueOf(payload.substring(0, p1));
        double amount = Double.parseDouble(payload.substring(p1 + 1, p2));
        long when = Long.parseLong(payload.substring(p2 + 1, p3));
        String detail = unescapeDetail(payload.substring(p3 + 1));

        return new Transaction(type, amount, when, detail);
    }

    private static String formatDouble(double v) {
        return String.format(Locale.US, "%s", Double.toString(v));
    }

    static String escapeField(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    static String unescapeField(String s) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);

                if (n == '\\' || n == '|') {
                    out.append(n);
                } else {
                    out.append(c).append(n);
                }
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    static String escapeDetail(String detail) {
        if (detail == null || detail.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < detail.length(); i++) {
            char c = detail.charAt(i);

            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    static String unescapeDetail(String s) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);

                switch (n) {
                    case '\\':
                        out.append('\\');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    default:
                        out.append(c).append(n);
                        break;
                }
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    public static final class BankSnapshot {
        private final Bank bank;
        private final String activeAccountId;

        public BankSnapshot(Bank bank, String activeAccountId) {
            this.bank = bank;
            this.activeAccountId = activeAccountId;
        }

        public Bank getBank() {
            return bank;
        }

        public String getActiveAccountId() {
            return activeAccountId;
        }
    }
}
