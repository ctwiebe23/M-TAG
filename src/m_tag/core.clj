(ns m-tag.core
  (:gen-class)
  (:require [m-tag.audio-util        :as aud]
            [m-tag.cli-validation    :as cli]
            [m-tag.jaudiotagger-util :as jat]))

;; GLOSSARY
;; - state
;;    Refers to the program state, a map of fields that contain information on
;;    on the current state of the program (i.e. a list of errors, the number of
;;    files processed, etc.).
;; - source
;;    Refers to the source directory that contains the target files.
;; - args & opts
;;    `args` refers to command line arguments, while `opts` refers to the given
;;    options/flags (i.e. -v).
;; - fields, fvals & splitter
;;    `fields` refers to different tag fields (i.e. artist). `fvals` holds the
;;    values of these fields. `splitter` holds a regex that is used to split
;;    the file name into different `fvals`.
;; - audio & file
;;    `file` refers to the Java file object, while `audio` refers to the
;;    JAudioTagger audio file object.
;; - file-info
;;    Refers to a map of information about a file (i.e. type, fvals, etc.).

(defn -main
  "Checks if the given arguments are valid via validate-args, if so then it
   runs process-audio on each file in the source directory, prints any errors,
   and prints a concluding statement.

   If '-s' present in :opts then it does not print any errors."
  [& args]
  (let [initial-state (cli/validate-args args)]
    (when initial-state
      (jat/configure-jat initial-state)
      (let [files      (. (initial-state :source) listFiles)
            file-infos (map #(aud/file->file-info initial-state %) files)
            end-state  (reduce aud/process-audio initial-state file-infos)]
        (when-not (some #{"-s"} (initial-state :opts))
          (run! println (end-state :errors)))
        (println "COMPLETE:" (end-state :tagged) "/" (end-state :total)
                 "files" (if (some #{"-t"} (initial-state :opts))
                           "passed testing"
                           "successfully tagged"))))))
