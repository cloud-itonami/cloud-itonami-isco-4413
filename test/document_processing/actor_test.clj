(ns document-processing.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [document-processing.actor :as actor]
            [document-processing.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :name "Client Intake Forms"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:batch-id "batch-1" :op :code :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "batch-1"))))))

(deftest holds-on-unregistered-batch-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:batch-id "no-such-batch" :op :code :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-batch")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; confidential-document handling always escalates (governor invariant)
        request {:batch-id "batch-1" :op :handle-confidential-document :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "batch-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "batch-1")))))))
