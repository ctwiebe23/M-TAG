(ns m-tag.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [m-tag.util      :as util])
  (:import  (org.jaudiotagger.tag TagOptionSingleton)))

;; A map of valid options and their descriptive strings.
(def opts-map
  {"-r" "Recursively operate on child directories"
   "-t" "Execute a test run without edits, printing files and current tags"
   "-v" "Print tag information for each file"
   "-s" "Silence errors"
   "-f" (str "Change the expected format; must be final option\n"
             "          Usage: -f <Splitter> [COMPONANTS] or -f <Componant>")})

(defn print-CLA-error
  "Prints a formatted error message regarding CLA input."
  [message]
  (println "ERROR:" message "\n"
           " Usage: <Filepath> [OPTIONS]\n"
           " Options:")
  (run! #(println "   " (first %) "" (second %)) (seq opts-map)))

(defn print-format-error
  "Prints a formatted error message regarding the given format."
  [message]
  (println "ERROR:" message "\n"
           " Usage: -f <Splitter> [COMPONANTS] or -f <COMPONANT>\n"
           "   <Splitter> will be read as a regex.\n"
           " Componants:")
  (run! #(println "   " (first %)) (seq util/componant-map)))

(def initial-state
  {:comps    ["title" "artist"]
   :splitter #" - "
   :tagged   0
   :total    0
   :errors   []})

(defn validate-format
  "Takes a program state and a collection of strings representing a format, 
   then returns a program state including the new format on a success but
   prints an error message and returns nil on a failure."
  [state raw-format]
  (cond
    (empty? raw-format)
    (print-format-error "No arguments given to -f")
    (= 1 (count raw-format))
    (if-not (util/componant-map (first raw-format))
      (print-format-error "Invalid componant given to -f")
      (merge state {:comps    raw-format
                    :splitter #"a^"}))
    (reduce #(and %1 (util/componant-map %2)) true (rest raw-format))
    (merge state {:comps    (rest raw-format)
                  :splitter (re-pattern (first raw-format))})
    :else
    (print-format-error "Invalid componant given to -f")))

(defn validate-opts
  "Validates each given option, if successful returns a program state, if 
   unsuccessful prints an error message and returns nil."
  [state opts]
    (loop [untested opts valid []]
      (cond
        (empty? untested)
        (merge state {:opts valid})
        (= "-f" (first untested))
        (validate-format (merge state {:opts valid}) (rest untested))
        (opts-map (first untested))
        (recur (rest untested) (cons (first untested) valid))
        :else
        (print-CLA-error "Invalid option given"))))

(defn validate-args
  "Checks if the first argument given is a directory and remaining arguments
   are valid options; returns a program state on a success, prints an error 
   message and returns nil on a failure."
  [args]
  (cond
    (empty? args)
    (print-CLA-error "No CLAs given")
    (not (.isDirectory (io/file (first args))))
    (print-CLA-error "Given filepath not valid")
    :else
    (validate-opts (merge initial-state {:source (io/file (first args))}) 
                   (rest args))))

(defn -main
  "Checks if the given arguments are valid via validate-args, if so then it runs
   process-audio on each file in the given directory, prints any errors, and 
   prints a concluding statement.
   
   If '-s' present in :opts then it does not print any errors."
  [& args]
  (let [user-input (validate-args args)]
    (when user-input
      (-> TagOptionSingleton
          (. getInstance)
          (. (setId3v2PaddingWillShorten true)))
      (let [files     (. (user-input :source) listFiles)
            end-state (reduce util/process-audio user-input files)]
        (when-not (some #{"-s"} (user-input :opts))
          (run! println (end-state :errors)))
        (println "COMPLETE:" (end-state :tagged) "/" (end-state :total)
                 "files" (if (some #{"-t"} (user-input :opts)) "passed testing"
                              "successfully tagged"))))))
