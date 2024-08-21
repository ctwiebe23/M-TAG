(ns m-tag.program-state
  (:gen-class))

;; Defines the initial program state that will be passed down and iterated on
;; by util's process-audio function via reduce.
(def initial-state
  {:source   nil                 ;; The directory that process-audio acts in.
   :opts     []                  ;; The user-specified options.
   :fields   ["title" "artist"]  ;; The componants that make up the filename.
   :splitter #" - "              ;; The regex that separates the componants.
   :tagged   0                   ;; The number of files successfully tagged.
   :total    0                   ;; The total number of audio files processed.
   :errors   []})                ;; A collection of errors regarding audio.

(defn failure
  [{total :total errors :errors :as state} message]
  (merge state {:total  (inc total)
                :errors (conj errors message)}))
