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
8. A bank administrator should be able to collect fees from existing accounts when necessary.
9. A bank administrator should be able to add an interest payment to an existing account when necessary.
10. A bank customer should be able to set a 4-digit PIN to protect their account and be required to authenticate when switching accounts. (Complete, Joel)
11. A bank operator should be able to store bank accounts and transaction history to disk so that restarting the app does not wipe customer data. (Complete, Daniel)
12. A bank customer should be able to filter their transaction history by type to make auditing easier. (Complete, Daniel)
13. A bank operator should be able to mark an account as frozen or unfrozen for purposes of security. (Complete, Adam)
14. A bank customer should be able to open different types of bank accounts (checking vs. saving), enforcing different rules. (Complete, Adam)
15. A bank customer should be able to generate an account statement covering a specific time frame.
16. A bank customer should be able to receive a warning when their balance drops below a minimum balance threshold after a withdrawal or transfer. (Complete, Joel)

## What user stories were completed this iteration?

- Story 10: 4-digit PIN at account creation, authentication when switching accounts, lockout after repeated failures; PIN state is persisted with bank data.
- Story 16: Warning when balance falls below the account’s minimum balance threshold (default $25; configurable, including 0 to disable); threshold is persisted.

## What user stories do you intend to complete next iteration?

15, plus other stories TBD. 

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
