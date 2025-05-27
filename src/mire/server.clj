(ns mire.server
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms])
  (:gen-class))

(defn- cleanup []
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @player/*inventory*]
     (commands/discard item))
   (commute player/streams dissoc player/*name*)
   (commute (:inhabitants @player/*current-room*)
            disj player/*name*)))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- get-special-greeting [name]
  "Returns a special greeting for certain names, or empty string if no special greeting."
  (let [lower-name (str/lower-case name)]
    (cond
      ;; Check for Alex variations
      (or (= lower-name "alex p")
          (= lower-name "alex prtutsky") 
          (= lower-name "alexander prutsky"))
      "Welcome sensei, hope you appreciate the work of your student!\n"
      
      ;; Check for Damir variations
      (or (= lower-name "damir k")
          (= lower-name "damir koblev"))
      "Welcome, master!\n"
      
      ;; No special greeting
      :else "")))

(defn- mire-handle-client [in out]
  (binding [*in* (io/reader in)
            *out* (io/writer out)
            *err* (io/writer System/err)]

    
    (print "\nWhat is your name? ") (flush)
    (binding [player/*name* (get-unique-player-name (read-line))
              player/*current-room* (ref (@rooms/rooms :start))
              player/*inventory* (ref #{})
              player/*exam-state* (ref :not-started)
              player/*exam-question* (ref 0)]
      
      ;; Display special greeting if applicable
      (let [greeting (get-special-greeting player/*name*)]
        (when (not= greeting "")
          (print greeting) (flush)))
      
      (dosync
       (commute (:inhabitants @player/*current-room*) conj player/*name*)
       (commute player/streams assoc player/*name* *out*))

      (println (commands/look)) (print player/prompt) (flush)

      (try (loop [input (read-line)]
             (when input
               (println (commands/execute input))
               (.flush *err*)
               (print player/prompt) (flush)
               (recur (read-line))))
           (finally (cleanup))))))

(defn -main
  ([port dir]
     (rooms/add-rooms dir)
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port))
  ([port] (-main port "resources/rooms"))
  ([] (-main 3333)))
