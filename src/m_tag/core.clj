(ns m-tag.core
  (:gen-class)
  (:require [m-tag.util           :refer [process-audio]]
            [m-tag.cli-validation :refer [validate-args]])
  (:import  (org.jaudiotagger.tag TagOptionSingleton)
            (java.util.logging    Logger
                                  Level)))

(defn configure-jat
  "Configure JAudioTagger so that it shrinks excess space allocated to tags if
   such space is present; also silences the JAudioTagger logger unless '-j' is
   present in :opts."
  [{opts :opts}]
  (.. TagOptionSingleton
      getInstance
      (setId3v2PaddingWillShorten true))
  (when-not (some #{"-j"} opts)
    (.. Logger
        (getLogger "org.jaudiotagger")
        (setLevel (. Level OFF)))))

(defn -main
  "Checks if the given arguments are valid via validate-args, if so then it
   runs process-audio on each file in the given directory, prints any errors,
   and prints a concluding statement.

   If '-s' present in :opts then it does not print any errors."
  [& args]
  (let [user-input (validate-args args)]
    (when user-input
      (configure-jat user-input)
      (let [files     (. (user-input :source) listFiles)
            end-state (reduce process-audio user-input files)]
        (when-not (some #{"-s"} (user-input :opts))
          (run! println (end-state :errors)))
        (println "COMPLETE:" (end-state :tagged) "/" (end-state :total)
                 "files" (if (some #{"-t"} (user-input :opts)) "passed testing"
                             "successfully tagged"))))))
