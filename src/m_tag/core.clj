(ns m-tag.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [m-tag.util      :as util])
  (:import  (org.jaudiotagger.tag TagOptionSingleton)
            (java.util.logging    Logger
                                  Level)))

;; A map of valid options and their descriptive strings.
(def opts-map
  {"-r" "Recursively operate on subfolders"
   "-t" "Execute a test run without edits, printing files and CURRENT tags"
   "-v" "Print tag information for each file (implied in -v)"
   "-s" "Silence errors"
   "-j" "Print the JAudioTagger log"
   "-c" "Clear tags"
   "-f" (str "Change the expected format; must be final option"
             "\n      "
             "USAGE: -f <splitter (regex)> <componants> or -f <componant>")})

;; Defines the initial program state that will be passed down and iterated on
;; by util's process-audio function via reduce.
(def initial-state
  {:source   nil                 ;; The directory that process-audio acts in.
   :opts     []                  ;; The user-specified options.
   :comps    ["title" "artist"]  ;; The componants that make up the filename.
   :splitter #" - "              ;; The regex that separates the componants.
   :tagged   0                   ;; The number of files successfully tagged.
   :total    0                   ;; The total number of audio files processed.
   :errors   []})                ;; A collection of errors regarding audio.

(defn print-CLA-error
  "Prints a formatted error message regarding CLA input."
  [message]
  (println message
           "\nUSAGE: <filepath> <options>"
           "\nOPTIONS:")
  (run! #(println (first %) "  " (second %)) (seq opts-map)))

(defn print-format-error
  "Prints a formatted error message regarding the given format."
  [message]
  (println message
           "\nUSAGE: -f <splitter (regex)> <componants> or -f <componant>"
           "\nCOMPONANTS:")
  (run! #(println " " (first %)) (seq util/Comp-map)))

(defn validate-format
  "Takes a program state and a collection of strings representing a format,
   then returns a program state including the new format on a success but
   prints an error message and returns nil on a failure."
  [state raw-format]
  (cond
    (empty? raw-format)
    (print-format-error "ERROR: No arguments given to -f")
    (= 1 (count raw-format))
    (if-not (util/Comp-map (first raw-format))
      (print-format-error "ERROR: Invalid componant given to -f")
      (merge state {:comps    raw-format
                    :splitter #"a^"}))
    (reduce #(and %1 (util/Comp-map %2)) true (rest raw-format))
    (merge state {:comps    (rest raw-format)
                  :splitter (re-pattern (first raw-format))})
    :else
    (print-format-error "ERROR: Invalid componant given to -f")))

(defn validate-opts
  "Validates each given option, if successful returns a program state, if
   unsuccessful prints an error message and returns nil."
  [{current-opts :opts :as state} raw-opts]
  (loop [untested raw-opts tested []]
    (cond
      (empty? untested)
      (merge state {:opts (conj tested current-opts)})
      (= "-f" (first untested))
      (validate-format (merge state {:opts (conj tested current-opts)})
                       (rest untested))
      (opts-map (first untested))
      (recur (rest untested) (cons (first untested) tested))
      :else
      (print-CLA-error "ERROR: Invalid option given"))))

(defn validate-args
  "Checks if the first argument given is a directory and remaining arguments
   are valid options; returns a program state on a success, prints an error
   message and returns nil on a failure."
  [args]
  (cond
    (empty? args)
    (print-CLA-error (str "M'TAG - Tag audio files based on their filenames "
                          "(i.e. 'Title - Artist.mp3')"
                          "\nDEFAULT FORMAT: splitter: \""
                          (initial-state :splitter) "\" componants: "
                          (initial-state :comps)))
    (not (.isDirectory (io/file (first args))))
    (print-CLA-error "ERROR: Given filepath not valid")
    :else
    (validate-opts (merge initial-state {:source (io/file (first args))})
                   (rest args))))

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
            end-state (reduce util/process-audio user-input files)]
        (when-not (some #{"-s"} (user-input :opts))
          (run! println (end-state :errors)))
        (println "COMPLETE:" (end-state :tagged) "/" (end-state :total)
                 "files" (if (some #{"-t"} (user-input :opts)) "passed testing"
                             "successfully tagged"))))))
