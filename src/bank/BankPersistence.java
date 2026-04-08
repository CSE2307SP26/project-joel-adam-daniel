package bank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BankPersistence {

    // Default file next to the working directory when the CLI exits normally
    public static final String DEFAULT_FILENAME = "bank-data.txt";

    // First line of every save file; load fails fast if this does not match
    static final String HEADER = "BANK_PERSIST_V1";

    private BankPersistence() {}

    public static Path defaultPath() {
        return Path.of(DEFAULT_FILENAME);
    }

    public static void save(Bank bank, String activeAccountId, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(HEADER).append('\n');
        sb.append("ACTIVE|").append(escapeField(activeAccountId)).append('\n');

        // One block per account: ACC line, TXN lines in order, ENDACC
        for (Account acc : bank.getAllAccounts()) {
            sb.append("ACC|")
                    .append(escapeField(acc.getAccountNumber()))
                    .append('|')
                    .append(formatDouble(acc.getBalance()))
                    .append('\n');

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
            // First run, or user deleted the file — caller seeds defaults
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

        if (lines.isEmpty() || !HEADER.equals(lines.get(0).trim())) {
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
            int balSep = accPayload.lastIndexOf('|');
            if (balSep < 0) {
                throw new IllegalArgumentException("Invalid ACC line.");
            }

            String accountId = unescapeField(accPayload.substring(0, balSep));
            double balance = Double.parseDouble(accPayload.substring(balSep + 1));
            i++;

            List<Transaction> txns = new ArrayList<>();

            while (i < lines.size()) {
                String txLine = lines.get(i++);

                if (txLine.trim().equals("ENDACC")) {
                    break;
                }

                txns.add(parseTxnLine(txLine));
            }

            bank.addAccount(Account.fromPersisted(accountId, balance, txns));
        }

        if (bank.getAccountCount() == 0) {
            throw new IllegalArgumentException("No accounts in file.");
        }

        // Stale ACTIVE id after manual edits — fall back to a real account
        if (bank.getAccount(activeAccountId) == null) {
            activeAccountId = bank.getAllAccounts().iterator().next().getAccountNumber();
        }

        return new BankSnapshot(bank, activeAccountId);
    }

    private static Transaction parseTxnLine(String line) {
        if (!line.startsWith("TXN|")) {
            throw new IllegalArgumentException("Invalid TXN line.");
        }

        // Type, amount, and whenMs are separated by |; detail may contain | so it is last
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
        // Keep decimal points predictable in the file regardless of default locale
        return String.format(Locale.US, "%s", Double.toString(v));
    }

    // Account ids (ACTIVE, ACC) can contain | only if escaped; detail uses escapeDetail
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

    // Transaction detail strings may include newlines or backslashes
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

    // In-memory bank plus which account the menu had selected when the file was saved
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
