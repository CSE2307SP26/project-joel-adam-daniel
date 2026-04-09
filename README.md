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
8. A bank operator should be able to store bank accounts and transaction history to disk so that restarting the app does not wipe customer data. (Complete, Daniel)
9. A bank customer should be able to filter their transaction history by type to make auditing easier. (Complete, Daniel)
10. A bank operator should be able to mark an account as frozen or unfrozen for purposes of security. (Complete, Adam)
11. A bank customer should be able to open different types of bank accounts (checking vs. saving), enforcing different rules. (Complete, Adam)
12. A bank customer should be able to generate an account statement covering a specific time frame.
13. A bank customer should be able to protect their bank account with a PIN. (Complete, Joel)
14. A bank customer should receive a warning when their balance drops below the minimum balance threshold after a withdrawal or transfer. (Complete, Joel)

## What user stories were completed this iteration?

- Story 13: 4-digit PIN login system. Each account is protected by a PIN set at creation. The app requires PIN entry when switching to another account. Accounts are locked after 3 failed attempts.
- Story 14: Minimum balance warning. After any withdrawal that brings the balance below the threshold (default $25.00), the app prints a warning showing the current balance and the threshold. The threshold is configurable per account.

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
bash./runTests.sh or

 bash runTests.sh
```
