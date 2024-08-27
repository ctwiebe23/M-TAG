(ns m-tag.program-state
  (:gen-class))

(def default-state
  "Defines the default program state that will be passed down and iterated on
   by audio-util's process-audio function via reduce."
  {:source   nil                 ;; The directory that process-audio acts in.
   :opts     []                  ;; The user-specified options.
   :fields   ["title" "artist"]  ;; The fields that make up the filename.
   :splitter #" - "              ;; The regex that separates the fields
   :tagged   0                   ;; The number of files successfully tagged.
   :total    0                   ;; The total number of files processed.
   :errors   []})                ;; A collection of error messages.

(defn failure
  "Returns a program state with the message appended to :errors and :total
   incremented."
  [{total :total errors :errors :as state} message]
  (merge state {:total  (inc total)
                :errors (conj errors message)}))
