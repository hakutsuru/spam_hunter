(ns batch_process.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.pprint])
  (:import (java.util UUID))
  (:gen-class))


;; [challenge] message specs
;; batch is a collection of messages
;; [{:email-address "foo@buzz.biz" :spam-score 0.25}...]
;; spec provided by challenge, email-domains revised to
;; avoid repo being found by search on quirky strings
(def email-domains
  #{"yondu.mail"
    "fistmail.com"
    "gjail.com"
    "photonmail.com"
    "kol.com"
    "zcloud.com"
    "egads.com"})

(def email-regex
  #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

(s/def ::email-address
  (s/with-gen
    (s/and string? #(re-matches email-regex %))
    #(->>
      (gen/tuple (gen/such-that not-empty (gen/string-alphanumeric))
                 (s/gen email-domains))
      (gen/fmap (fn [[addr domain]] (str addr "@" domain))))))

(s/def ::spam-score
  (s/double-in :min 0 :max 1))

(s/def ::email-record
  (s/keys :req-un [::email-address ::spam-score]))


;; [response] batch handling logic
;; configuration
(def max-batch-mean-spam-score 0.05)
(def max-window-mean-spam-score 0.10)
(def max-email-mean-spam-score 0.30)
(def max-window-size 100)

;; uuid and mean calculations
(defn get-uuid []
  (.toString (UUID/randomUUID)))

(defn candidate-mean [msg state]
  (/ (+ (:spam-score msg) (:sum-spam-score state))
      (inc (:count-sent state))))

(defn window-mean [coll]
  (/ (apply + (map :spam-score coll))
     (count coll)))

;; manage message buffer
;; assumption: vector is trimmed after each update
;; so trimming must remove only first element
(defn update-window [msg window max-size]
  (let [candidate-window (conj window msg)]
   (if (> (count candidate-window) max-size)
     (subvec candidate-window 1)
     candidate-window)))

;; [mock] queue message
;; uuids illustrates yagni aesthetic
;; writing to queue may require async
(defn queue-email [msg batch-id event-id]
  (println
    (format ">>> selling herbal supplements to <%s>" (:email-address msg))))

;; reap and spam
(defn process-batch [batch state]
  (doseq [message batch]
    (swap! state update-in [:count-total] inc)
    (cond
      (contains? (:recipients @state) (:email-address message))
        (swap! state update-in [:reject-count-duplicate-addr] inc)
      (> (:spam-score message)  (:cfg-limit-score-message @state))
        (swap! state update-in [:reject-count-message-score] inc)
      (> (candidate-mean message @state) (:cfg-limit-score-batch @state))
        (swap! state update-in [:reject-count-batch-score] inc)
      :else
        ;; check of window spam score could be done in condition
        ;; but avoid the work of buffer management when most
        ;; messages will be rejected by earlier testing
        (let [candidate-window (update-window message (:window @state) (:cfg-window-size @state))]
          (if (> (window-mean candidate-window) (:cfg-limit-score-window @state))
            (swap! state update-in [:reject-count-window-score] inc)
            (do
              ;;(println candidate-window)
              (swap! state update-in [:count-sent] inc)
              (swap! state update-in [:recipients] #(conj % (:email-address message)))
              (swap! state update-in [:sum-spam-score] #(+ % (:spam-score message)))
              (swap! state assoc :window candidate-window)
              ;; dragons & side-effects
              (queue-email message (:batch-id @state) (get-uuid))))))))


(defn -main
  "revolutionary batch processing for great good"
  [& args]
  
  ;; obtain batch-size
  (def batch-size (Long/valueOf (first args)))

  ;; initialize batch state
  (def state (atom {
    :batch-id (get-uuid)
    :window []
    :count-sent 0
    :count-total 0
    :sum-spam-score 0
    :recipients #{}
    :reject-count-duplicate-addr 0
    :reject-count-message-score 0
    :reject-count-batch-score 0
    :reject-count-window-score 0
    :cfg-limit-score-message max-email-mean-spam-score
    :cfg-limit-score-batch max-batch-mean-spam-score
    :cfg-limit-score-window max-window-mean-spam-score
    :cfg-window-size max-window-size
  }))
  
  ;; [mock] obtain messages to process
  ;; use clojure.spec and test.check
  (def batch (gen/sample (s/gen ::email-record) batch-size))
  
  ;; testing hack
  ;; if you wish test.check to prove your dup filter works
  ;; you could be dead by the time any dup is generated
  ;;(def batch (conj batch {:email-address "bleh@testme.org" :spam-score 0.001}))
  ;;(def batch (conj batch {:email-address "bleh@testme.org" :spam-score 0.001}))
  ;;(def batch (conj batch {:email-address "bleh@testme.org" :spam-score 0.001}))

  ;; process batch
  (process-batch batch state)

  ;; report results
  (swap! state dissoc :window)
  (swap! state dissoc :recipients)
  (swap! state dissoc :sum-spam-score)
  (clojure.pprint/pprint @state))
