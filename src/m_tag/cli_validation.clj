(ns m-tag.cli-validation
  (:gen-class)
  (:require [clojure.java.io :as io]
            [m-tag.constants :refer [initial-state
                                     opts-map
                                     Comp-map]]))

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
  (run! #(println " " (first %)) (seq Comp-map)))

(defn validate-format
  "Takes a program state and a collection of strings representing a format,
   then returns a program state including the new format on a success but
   prints an error message and returns nil on a failure."
  [state raw-format]
  (cond
    (empty? raw-format)
    (print-format-error "ERROR: No arguments given to -f")
    (= 1 (count raw-format))
    (if-not (Comp-map (first raw-format))
      (print-format-error "ERROR: Invalid componant given to -f")
      (merge state {:comps    raw-format
                    :splitter #"a^"}))
    (reduce #(and %1 (Comp-map %2)) true (rest raw-format))
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
