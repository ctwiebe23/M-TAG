(ns m-tag.constants
  (:gen-class)
  (:import (org.jaudiotagger.tag FieldKey)))

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

;; A map of valid options and their descriptive strings.
(def opts-map
  {"-r" "Recursively operate on subfolders"
   "-t" "Execute a test run without edits, printing files and CURRENT tags"
   "-v" "Print tag information for each file (implied in -t)"
   "-s" "Silence errors"
   "-j" "Print the JAudioTagger log"
   "-c" "Clear tags"
   "-f" (str "Change the expected format; must be final option"
             "\n      "
             "USAGE: -f <splitter (regex)> <componants> or -f <componant>")})

;; A componant record.
(defrecord Comp
           [name field-key str-size])

;; Additional fields can be found in the JAudioTagger javadoc.
(def Comp-map
  {"album"        (Comp. "album"         FieldKey/ALBUM        "%-30s")
   "album_artist" (Comp. "album_artist"  FieldKey/ALBUM_ARTIST "%-50s")
   "artist"       (Comp. "artist"        FieldKey/ARTIST       "%-50s")
   "disc_number"  (Comp. "disc_number"   FieldKey/DISC_NO      "%-3s")
   "total_discs"  (Comp. "total_discs"   FieldKey/DISC_TOTAL   "%-3s")
   "title"        (Comp. "title"         FieldKey/TITLE        "%-50s")
   "track"        (Comp. "track"         FieldKey/TRACK        "%-3s")
   "total_tracks" (Comp. "total_tracks"  FieldKey/TRACK_TOTAL  "%-3s")
   "year"         (Comp. "year"          FieldKey/YEAR         "%-5s")})

(def supported-types
  ["mp3" "wav" "ogg" "flac"])
