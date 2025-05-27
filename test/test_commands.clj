(ns test-commands
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [mire.commands :refer :all]
            [mire.player :as player]
            [mire.rooms :as rooms]))

(rooms/add-rooms "resources/rooms/")

(defmacro def-command-test [name & body]
  `(deftest ~name
     (binding [player/*current-room* (ref (:start @rooms/rooms))
               player/*inventory* (ref #{})
               player/*name* "Tester"
               player/*exam-state* (ref :not-started)
               player/*exam-question* (ref 0)]
       ~@body)))

(def-command-test test-execute
  ;; Silence the error!
  (binding [*err* (io/writer "/dev/null")]
    (is (= "You can't do that!"
           (execute "discard a can of beans into the fridge"))))
  (is (re-find #"closet" (execute "north")))
  (is (= @player/*current-room* (:closet @rooms/rooms))))

(def-command-test test-move
  (is (re-find #"hallway" (execute "south")))
  (is (re-find #"Alexander Prutsky" (move "east")))
  (is (re-find #"can't go that way" (move "south"))))

(def-command-test test-look
  (binding [player/*current-room* (ref (:closet @rooms/rooms))]
    (doseq [look-for [#"closet" #"keys" #"south"]]
    (is (re-find look-for (look))))))

(def-command-test test-inventory
  (binding [player/*inventory* (ref [:keys :bunny])]
    (is (re-find #"bunny" (inventory)))
    (is (re-find #"keys" (inventory)))))

(def-command-test test-grab
  (binding [player/*current-room* (ref (:closet @rooms/rooms))]
    (is (not (= "There isn't any keys here"
                (grab "keys"))))
    (is (player/carrying? :keys))
    (is (empty? @(@player/*current-room* :items)))))

(def-command-test test-discard
  (binding [player/*inventory* (ref #{:bunny})]
    (is (re-find #"dropped" (discard "bunny")))
    (is (not (player/carrying? "bunny")))
    (is (rooms/room-contains? @player/*current-room* "bunny"))))

;; New functionality tests

(def-command-test test-coke-command
  (is (= "Aaaah, now we're talking" (execute "coke"))))

(def-command-test test-map-command
  (let [map-output (execute "map")]
    (is (re-find #"Game World Map" map-output))
    (is (re-find #"closet" map-output))
    (is (re-find #"start" map-output))
    (is (re-find #"hallway" map-output))
    (is (re-find #"102" map-output))
    (is (re-find #"promenade" map-output))
    (is (re-find #"Current location" map-output))))

(def-command-test test-room-102-navigation
  ;; Test navigation to room 102
  (execute "south") ; Go to hallway
  (let [result (execute "east")] ; Go to room 102
    (is (re-find #"Alexander Prutsky" result))
    (is (re-find #"Welcome to the Clojure Exam" result))
    (is (re-find #"What is the result of.*1 2 3" result))))

(def-command-test test-exam-system-start
  ;; Navigate to room 102 and check exam starts
  (execute "south") ; hallway
  (execute "east")  ; room 102
  (is (= @player/*exam-state* :in-progress))
  (is (= @player/*exam-question* 0)))

(def-command-test test-exam-correct-answers
  ;; Test complete exam flow with correct answers
  (binding [player/*current-room* (ref (:102 @rooms/rooms))
            player/*exam-state* (ref :in-progress)
            player/*exam-question* (ref 0)]
    ;; Answer question 1
    (let [result1 (execute "6")]
      (is (re-find #"Correct" result1))
      (is (re-find #"Question 2" result1))
      (is (= @player/*exam-question* 1)))
    
    ;; Answer question 2  
    (let [result2 (execute "cons")]
      (is (re-find #"Correct" result2))
      (is (re-find #"Question 3" result2))
      (is (= @player/*exam-question* 2)))
    
    ;; Answer question 3
    (let [result3 (execute "1")]
      (is (re-find #"Correct" result3))
      (is (re-find #"passed the Clojure exam" result3))
      (is (= @player/*exam-state* :passed)))))

(def-command-test test-exam-incorrect-answer
  ;; Test exam failure
  (binding [player/*exam-state* (ref :in-progress)
            player/*exam-question* (ref 0)]
    (let [result (execute "wrong")]
      (is (re-find #"Incorrect" result))
      (is (re-find #"Don't give up" result))
      (is (= @player/*exam-state* :failed)))))

(def-command-test test-exam-retake
  ;; Test retaking exam after failure
  (execute "south") ; hallway
  (execute "east")  ; room 102 (first time)
  (execute "wrong") ; fail exam
  (execute "west")  ; leave room
  (let [result (execute "east")] ; re-enter room
    (is (re-find #"Alexander Prutsky" result))
    (is (re-find #"Question 1" result))
    (is (= @player/*exam-state* :in-progress))))

(def-command-test test-exam-already-passed
  ;; Test entering room after passing exam
  (binding [player/*exam-state* (ref :passed)]
    (execute "south") ; hallway
    (let [result (execute "east")] ; room 102
      (is (re-find #"already passed" result))
      (is (re-find #"Well done" result)))))

(def-command-test test-exam-natural-input
  ;; Test that normal commands still work during exam
  (binding [player/*exam-state* (ref :in-progress)]
    (is (re-find #"cramped closet|round room|hallway|room 102" (execute "look")))
    (is (re-find #"You are carrying" (execute "inventory")))
    (is (re-find #"Game World Map" (execute "map")))))

(def-command-test test-answer-command-still-works
  ;; Test that explicit answer command still works
  (binding [player/*exam-state* (ref :in-progress)
            player/*exam-question* (ref 0)]
    (let [result (execute "answer 6")]
      (is (re-find #"Correct" result)))))
