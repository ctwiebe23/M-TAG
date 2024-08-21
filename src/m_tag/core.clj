(ns m-tag.core
  (:gen-class)
  (:require [m-tag.audio-util        :as aud]
            [m-tag.cli-validation    :as cli]
            [m-tag.jaudiotagger-util :as jat]))

(defn -main
  "Checks if the given arguments are valid via validate-args, if so then it
   runs process-audio on each file in the given directory, prints any errors,
   and prints a concluding statement.

   If '-s' present in :opts then it does not print any errors."
  [& args]
  (let [user-input (cli/validate-args args)]
    (when user-input
      (jat/configure-jat user-input)
      (let [files     (. (user-input :source) listFiles)
            end-state (reduce aud/process-audio user-input files)]
        (when-not (some #{"-s"} (user-input :opts))
          (run! println (end-state :errors)))
        (println "COMPLETE:" (end-state :tagged) "/" (end-state :total)
                 "files" (if (some #{"-t"} (user-input :opts)) "passed testing"
                             "successfully tagged"))))))
