# project26

## Team Members:

* Adam Brook
* Daniel Huinda
* Joel Aycock

## User stories

1. A bank customer should be able to deposit into an existing account. (Shook)
2. A bank customer should be able to withdraw from an account.  (Complete, Daniel)
3. A bank customer should be able to check their account balance. (Complete, Adam)
4. A bank customer should be able to view their transaction history for an account. (Complete, Daniel)
5. A bank customer should be able to create an additional account with the bank. (Complete, Adam)
6. A bank customer should be able to close an existing account. (Complete, Joel)
7. A bank customer should be able to transfer money from one account to another.  (Complete, Joel)
8. A bank operator should be able to store bank accounts and transaction history to disk so that restarting the app does not wipe customer data. Saves use **`BANK_PERSIST_V4`** (`bank-data.txt` by default): active account, each account’s balance, type, savings period fields (when applicable), frozen flag, **4-digit PIN**, failed-attempt count, lock state, **minimum balance threshold**, and full transaction history. Older files (`V1`–`V3`) still load; missing PIN or threshold fields default to **PIN 1234** and **$25.00** until the data is re-saved from this version. (Complete, Daniel)
9. A bank customer should be able to filter their transaction history by type to make auditing easier. (Complete, Daniel)
10. A bank operator should be able to mark an account as frozen or unfrozen for purposes of security. (Complete, Adam)
11. A bank customer should be able to open different types of bank accounts (checking vs. saving), enforcing different rules. (Complete, Adam)
12. A bank customer should be able to generate an account statement covering a specific time frame.
13. A bank customer should be able to protect their bank account with a PIN (4 digits, lockout after failed attempts). PIN verification is required when switching accounts; PIN and lockout state persist in **`BANK_PERSIST_V4`**. (Complete, Joel)
14. A bank customer should receive a warning when their balance drops below the minimum balance threshold after a withdrawal or transfer; the threshold is per-account and persisted. (Complete, Joel)

## What user stories were completed this iteration?

- Story 13: 4-digit PIN login system. Each account is protected by a PIN set at creation (and when creating a brand-new bank from the CLI). The app requires PIN entry when switching to another account (skipped if the target is already active). Accounts lock after 3 failed attempts; **failed-attempt count and lock state** are written and restored with **`BANK_PERSIST_V4`**.
- Story 14: Minimum balance warning. After a withdrawal or **outgoing transfer** that brings the balance below the threshold (default **$25.00**), the app prints a warning with the new balance and the threshold. The threshold is configurable per account and **saved in `BANK_PERSIST_V4`**. A threshold of **0** disables warnings.

## What user stories do you intend to complete next iteration?

N/A

## Is there anything that you implemented but doesn't currently work?

N/A

## What commands are needed to compile and run your code from the command line?

Run the app:

```
bash ./runApp.sh

or  bash runApp.sh
```

Run unit tests:

```
bash ./runTests.sh
```
