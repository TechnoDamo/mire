(ns test-rooms
  (:require [mire.rooms :refer :all]
            [clojure.test :refer :all]))

(defn room-fixture [f]
  (with-redefs [rooms (atom (load-rooms {} "resources/rooms/"))]
    (f)))

(use-fixtures :each room-fixture)

(deftest test-set-rooms
  (doseq [name [:start :closet :hallway :promenade :102]]
    (is (contains? @rooms name)))
  (is (re-find #"promenade" (:desc (:promenade @rooms))))
  (is (= :hallway (:north @(:exits (:promenade @rooms)))))
  (is (some #{:bunny} @(:items (:promenade @rooms))))
  (is (empty? @(:inhabitants (:promenade @rooms)))))

(deftest test-room-contains?
  (let [closet (:closet @rooms)]
    (is (not (empty? (filter #(= % :keys) @(:items closet)))))
    (is (room-contains? closet "keys"))
    (is (not (room-contains? closet "monkey")))))

;; New room tests

(deftest test-room-102-exists
  (is (contains? @rooms :102))
  (let [room-102 (:102 @rooms)]
    (is (re-find #"room 102" (:desc room-102)))
    (is (= :hallway (:west @(:exits room-102))))
    (is (empty? @(:items room-102)))))

(deftest test-hallway-connections
  (let [hallway (:hallway @rooms)
        exits @(:exits hallway)]
    (is (= :start (:north exits)))
    (is (= :102 (:east exits)))
    (is (= :promenade (:south exits)))
    (is (some #{:detector} @(:items hallway)))))

(deftest test-room-navigation-integrity
  ;; Test that all room connections are bidirectional where expected
  (let [start (:start @rooms)
        hallway (:hallway @rooms)
        closet (:closet @rooms)
        promenade (:promenade @rooms)
        room-102 (:102 @rooms)]
    
    ;; Start <-> Hallway
    (is (= :hallway (:south @(:exits start))))
    (is (= :start (:north @(:exits hallway))))
    
    ;; Start <-> Closet  
    (is (= :closet (:north @(:exits start))))
    (is (= :start (:south @(:exits closet))))
    
    ;; Hallway <-> Promenade
    (is (= :promenade (:south @(:exits hallway))))
    (is (= :hallway (:north @(:exits promenade))))
    
    ;; Hallway <-> Room 102
    (is (= :102 (:east @(:exits hallway))))
    (is (= :hallway (:west @(:exits room-102))))))

(deftest test-room-descriptions
  (is (re-find #"round room.*pillar" (:desc (:start @rooms))))
  (is (re-find #"cramped closet" (:desc (:closet @rooms))))
  (is (re-find #"long.*hallway" (:desc (:hallway @rooms))))
  (is (re-find #"promenade stretches" (:desc (:promenade @rooms))))
  (is (re-find #"room 102.*quiet room" (:desc (:102 @rooms)))))
