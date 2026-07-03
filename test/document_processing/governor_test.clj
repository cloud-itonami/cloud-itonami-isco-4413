(ns document-processing.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [document-processing.store :as store]
            [document-processing.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :name "Client Intake Forms"})
    st))

(deftest ok-on-clean-code
  (let [st (fresh-store)
        proposal {:op :code :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-batch
  (let [st (fresh-store)
        proposal {:op :code :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "no-such-batch"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-batch (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :code :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-confidential-document-handling
  (let [st (fresh-store)
        proposal {:op :handle-confidential-document :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-regulated-document-handling
  (let [st (fresh-store)
        proposal {:op :handle-regulated-document :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :code :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:batch-id "batch-1" :op :proofread})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "batch-1"))))
    (is (= 1 (count (store/ledger st))))))
