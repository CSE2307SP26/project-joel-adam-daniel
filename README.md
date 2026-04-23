# project26

## Team Members:

* Adam Brook
* Daniel Huinda
* Joel Aycock

## User stories

1. A bank customer should be able to deposit into an existing account. (Complete)
2. A bank customer should be able to withdraw from an account. (Complete, Daniel)
3. A bank customer should be able to check their account balance. (Complete, Adam)
4. A bank customer should be able to view their transaction history for an account. (Complete, Daniel)
5. A bank customer should be able to create an additional account with the bank. (Complete, Adam)
6. A bank customer should be able to close an existing account. (Complete, Joel)
7. A bank customer should be able to transfer money from one account to another. (Complete, Joel)
8. A bank administrator should be able to collect fees from existing accounts when necessary. (Complete, Daniel)
9. A bank administrator should be able to add an interest payment to an existing account when necessary. (Complete, Daniel)
10. A bank customer should be able to set a 4-digit PIN to protect their account and be required to authenticate when switching accounts. (Complete, Joel)
11. A bank operator should be able to store bank accounts and transaction history to disk so that restarting the app does not wipe customer data. (Complete, Daniel)
12. A bank customer should be able to filter their transaction history by type to make auditing easier. (Complete, Daniel)
13. A bank operator should be able to mark an account as frozen or unfrozen for purposes of security. (Complete, Adam)
14. A bank customer should be able to open different types of bank accounts (checking vs. saving), enforcing different rules. (Complete, Adam)
15. A bank customer should be able to generate an account statement covering a specific time frame. (Complete, Adam)
16. A bank customer should be able to receive a warning when their balance drops below a minimum balance threshold after a withdrawal or transfer. (Complete, Joel)
17. A bank customer should be able to schedule a recurring transfer between two of their accounts so that money moves automatically without manual action each period. (Complete, Joel)
18. A bank customer should be able to view a spending summary for an account that breaks down total deposits vs. total withdrawals over a given time period. (Complete, Joel)

### How newer stories map to the app

* **8 — Fees:** Menu **15 — Operator — collect fee from account** (`Bank.collectFee`). Posts a `FEE` transaction; does not count toward the savings monthly withdrawal limit.
* **9 — Interest:** Menu **16 — Operator — apply interest to account** (`Bank.applyInterest`). Posts an `INTEREST` transaction.
* **15 — Statement:** Menu **14 — Account statement (by date range)** for the **active** account. Lists all transactions (including fees and interest) between the chosen dates, inclusive of calendar days (same midnight-to-midnight convention as the spending summary).

## What user stories were completed this iteration?

8, 9, 15 (administrator fee collection, interest credits, and date-range account statements), plus ongoing test coverage for bank and persistence behavior.

## What user stories do you intend to complete next iteration?

Further polish or new stories as assigned by the course (all listed user stories above are implemented).

## Is there anything that you implemented but doesn't currently work?

N/A

## What commands are needed to compile and run your code from the command line?

Run the app:

```
bash ./runApp.sh
```

Run unit tests:

```
bash ./runTests.sh
```

## Branching note

Feature work for the remaining stories was done on branch `feature/stories-8-9-15` (from `development`). Merge via pull request into `development` following team workflow.
