batch_process
====


## Orientation

To demonstrate data processing via Clojure, we use Clojure.spec to generate a collection of potential email recipients.

```
{:email-address "direwolf@zcloud.com"
 :spam-score 0.088}
```

The batch of potential emails must be filtered to ensure emails would not violate any of these spam-related rules...

[1] Only send one email per address.<br />
[2] Reject any email with spam score higher than 0.3.<br />
[3] Reject email that boosts average spam score above 0.05 (for sent emails).<br />
[4] Reject email that boosts average spam score above 0.10 for the last 100 emails.

## Environment

Expect to provide a vagrant setup for testing, otherwise...

```
$ lein --version
Leiningen 2.8.1 on Java 9.0.4 Java HotSpot(TM) 64-Bit Server VM
$ java -version
java version "9.0.4"
Java(TM) SE Runtime Environment (build 9.0.4+11)
Java HotSpot(TM) 64-Bit Server VM (build 9.0.4+11, mixed mode)
```

## Usage

Clone `spam_hunter` project.

Navigate to batch_process directory...

```
 ~/Desktop/spam_hunter/batch_process $ lein run 32
>>> selling herbal supplements to <M11Oc@egads.com>
>>> selling herbal supplements to <y7Gj1k29ny78mW@fistmail.com>
>>> selling herbal supplements to <J1U5dS@zcloud.com>
>>> selling herbal supplements to <l8OG2Q1c708K5b50W3vu4GLTEcV15b0@photonmail.com>
{:count-sent 4,
 :cfg-window-size 100,
 :reject-count-batch-score 5,
 :count-total 32,
 :cfg-limit-score-batch 0.05,
 :reject-count-message-score 23,
 :cfg-limit-score-message 0.3,
 :cfg-limit-score-window 0.1,
 :reject-count-duplicate-addr 0,
 :reject-count-window-score 0,
 :batch-id "e3bfe7ab-19bd-4869-998e-04552b76246d"}
```
