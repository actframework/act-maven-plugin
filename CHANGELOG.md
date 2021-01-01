# ACT Maven Plugin

## 1.9.1.0

* Update act maven parent to 1.9.1a
* Exception when running `run_dev` in a headless environment #7

## 1.8.27.0
* lombok support

## 1.8.26.0
* Disable JVM remote debugging when run test #5

## 1.8.12.0 - 20/Nov/2018
* Bind `act:test` to maven `test` phase #4
* Update to act-1.8.12

## 1.8.8.8 - 30/Oct/2018
- rename e2e to test

## 1.8.8.3 - 19/Jun/2018
- act-e2e: exit to system with exit code inherited from e2e process #3

## 1.8.8.2 - 29/May/2018
- When no profile specified `act:e2e` shall use `e2e` as profile

## 1.8.8.1 - 29/May/2018
- update logic to trigger `act:e2e` based on act-e2e change #2
- Allow passing `profile` into act runner #1

## 1.8.8.0
- Add `act:e2e` command

## 1.8.5.0 - 03/Apr/2018
- First commit
