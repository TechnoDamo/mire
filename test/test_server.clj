(ns test-server
  (:require [clojure.test :refer :all]
            [mire.server :refer :all]))

(deftest test-special-greeting-alex-variations
  ;; Test Alex Prutsky variations
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "Alex P")))
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "alex p")))
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "ALEX P")))
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "Alex Prtutsky")))
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "Alexander Prutsky")))
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "alexander prutsky"))))

(deftest test-special-greeting-damir-variations
  ;; Test Damir Koblev variations
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "Damir K")))
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "damir k")))
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "DAMIR K")))
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "Damir Koblev")))
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "damir koblev"))))

(deftest test-special-greeting-case-insensitive
  ;; Test case insensitivity
  (is (= "Welcome sensei, hope you appreciate the work of your student!\n"
         (#'mire.server/get-special-greeting "aLeX p")))
  (is (= "Welcome, master!\n"
         (#'mire.server/get-special-greeting "DaMiR kObLeV"))))

(deftest test-no-special-greeting
  ;; Test that regular names get no special greeting
  (is (= "" (#'mire.server/get-special-greeting "John Doe")))
  (is (= "" (#'mire.server/get-special-greeting "Jane Smith")))
  (is (= "" (#'mire.server/get-special-greeting "Alex")))
  (is (= "" (#'mire.server/get-special-greeting "Damir")))
  (is (= "" (#'mire.server/get-special-greeting "Random Player"))))

(deftest test-edge-cases
  ;; Test edge cases
  (is (= "" (#'mire.server/get-special-greeting "")))
  (is (= "" (#'mire.server/get-special-greeting "   ")))
  (is (= "" (#'mire.server/get-special-greeting "Alexander")))
  (is (= "" (#'mire.server/get-special-greeting "Prutsky")))) 