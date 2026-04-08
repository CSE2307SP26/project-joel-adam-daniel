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
8. A bank adminstrator should be able to collect fees from existing accounts when necessary.
9. A bank adminstrator should be able to add an interest payment to an existing account when necessary.

## What user stories do you intend to complete next iteration?

- 8 and 9 (admin fee collection and interest payment).

### Two tasks per person (next iteration)

| Person | Task 1 | Task 2 |
|--------|--------|--------|
| **Adam** | Story 8: fee collection in `bank` domain + menu entry | Story 9: interest accrual API + tests with edge cases |
| **Daniel** | End-to-end tests / regression for admin features | Documentation and `runTests.sh` / CI notes for graders |
| **Joel** | Story 8/9 UX flows in `MainMenu` (prompts, validation) | Repository hygiene: delete merged feature branches after PR merge |

**Branching:** After a PR is merged, delete the remote/local feature branch if it is no longer needed to keep the repo tidy.

## Is there anything that you implemented but doesn't currently work?

N/A

## What commands are needed to compile and run your code from the command line?

Run the app:

```bash
./runApp.sh
```

Run unit tests:

```bash
./runTests.sh
```
