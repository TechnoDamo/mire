(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.player :as player]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Exam system
(def exam-questions
  [{:question "What is the result of (+ 1 2 3)?"
    :answers ["6" "6.0" "six"]
    :correct-answer "6"}
   {:question "What function is used to add an element to the beginning of a list?"
    :answers ["cons" "conj"]
    :correct-answer "cons"}
   {:question "What does (first [1 2 3]) return?"
    :answers ["1" "1.0" "one"]
    :correct-answer "1"}])

(defn start-exam []
  "Start the Clojure exam."
  (dosync
    (ref-set player/*exam-state* :in-progress)
    (ref-set player/*exam-question* 0))
  "Hello! I am Alexander Prutsky, the master of the functional and recursive programming.\n\nWelcome to the Clojure Exam! Answer the following questions:\n\nQuestion 1: What is the result of (+ 1 2 3)?")

(defn check-exam-answer [answer]
  "Check if the exam answer is correct and proceed."
  (let [current-q @player/*exam-question*
        question (nth exam-questions current-q)
        correct? (some #(= (str/lower-case %) (str/lower-case answer)) (:answers question))]
    (if correct?
      (let [next-q (inc current-q)]
        (dosync (ref-set player/*exam-question* next-q))
        (cond
          (= next-q 1) "Correct! Question 2: What function is used to add an element to the beginning of a list?"
          (= next-q 2) "Correct! Question 3: What does (first [1 2 3]) return?"
          (= next-q 3) (do (dosync (ref-set player/*exam-state* :passed))
                          "Correct! Congratulations! You have passed the Clojure exam!")
          :else "Error in exam system."))
      (do (dosync (ref-set player/*exam-state* :failed))
          "Incorrect answer. Don't give up! Try entering the room again to retake the exam."))))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str (:desc @player/*current-room*)
       "\nExits: " (keys @(:exits @player/*current-room*)) "\n"
       (str/join "\n" (map #(str "There is " % " here.\n")
                           @(:items @player/*current-room*)))))

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @player/*current-room*) (keyword direction))
         target (@rooms/rooms target-name)]
     (if target
       (do
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants target))
         (ref-set player/*current-room* target)
         ;; Special handling for room 102 (exam room)
         (if (= target-name :102)
           (cond
             (= @player/*exam-state* :passed) (str (look) "\n\nYou have already passed the exam! Well done!")
             (or (= @player/*exam-state* :not-started) (= @player/*exam-state* :failed)) (start-exam)
             :else (look))
           (look)))
       "You can't go that way."))))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (rooms/room-contains? @player/*current-room* thing)
     (do (move-between-refs (keyword thing)
                            (:items @player/*current-room*)
                            player/*inventory*)
         (str "You picked up the " thing "."))
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\n"
       (str/join "\n" (seq @player/*inventory*))))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str/join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))

(defn coke
  "Sounds of enjoyment of a refreshing beverage."
  []
  "Aaaah, now we're talking!)")

(defn map
  "Display a visual map of the game world."
  []
  (str "Game World Map:\n\n"
       "      [closet]      \n"
       "          |         \n"
       "      [start]       \n" 
       "          |         \n"
       "     [hallway] -- [102]\n"
       "          |         \n"
       "    [promenade]     \n\n"
       "Current location: " (:name @player/*current-room*) "\n"
       ))

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help
               "coke" coke
               "map" map
               "answer" check-exam-answer})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         ;; Check if player is taking exam and input is not a recognized command
         (if (and (= @player/*exam-state* :in-progress)
                  (not (contains? commands command)))
           ;; Treat the entire input as an exam answer
           (check-exam-answer (str/trim input))
           ;; Normal command execution
           (apply (commands command) args)))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
