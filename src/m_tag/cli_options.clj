(ns m-tag.cli-options
  (:gen-class))

;; A map of valid options and their descriptive strings.
(def opt-map
  {"-r" "Recursively operate on subfolders"
   "-t" "Execute a test run without edits, printing files and CURRENT tags"
   "-v" "Print tag information for each file (implied in -t)"
   "-s" "Silence errors"
   "-j" "Print the JAudioTagger log"
   "-c" "Clear tags"
   "-f" (str "Change the expected format; must be final option"
             "\n      "
             "USAGE: -f <splitter (regex)> <componants> or -f <componant>")})
