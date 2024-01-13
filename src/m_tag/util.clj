(ns m-tag.util
  (:require [clojure.string :as str])
  (:import  (org.jaudiotagger.audio AudioFileIO)
            (org.jaudiotagger.tag   FieldKey)))

;; Additional fields can be found in the JAudioTagger javadoc.
(def componant-map
  {"album"        FieldKey/ALBUM
   "album_artist" FieldKey/ALBUM_ARTIST
   "artist"       FieldKey/ARTIST
   "disc_number"  FieldKey/DISC_NO
   "total_discs"  FieldKey/DISC_TOTAL
   "title"        FieldKey/TITLE
   "track"        FieldKey/TRACK
   "total_tracks" FieldKey/TRACK_TOTAL
   "year"         FieldKey/YEAR})

(def supported-types
  ["mp3" "wav" "ogg" "flac"])

(defn print-tag
  "Prints the filepath of the given file relative to the source filepath, and 
   then prints the current values of the relevant portions of the file's audio
   tag."
  [file path comps]
  (let [audio (AudioFileIO/read file)
        tag   (. audio getTagOrCreateDefault)]
    (println path)
    (run! #(printf "%s: %s    " % (->> % componant-map getFirst (. tag))) 
          comps)
    (println "\n")))

(defn set-tag
  "Sets the tag of the given audio file according to the given vals and the
   list of componants defined in tag-format."
  [file vals comps]
  (let [audio (AudioFileIO/read file)
        tag   (. audio getTagOrCreateAndSetDefault)] 
    (loop [i 0]
      (when (< i (count comps))
        (. tag (setField (-> comps (nth i) componant-map)
                         (->> (nth vals i) vector (into-array String))))
        (recur (inc i))))
    (AudioFileIO/write audio)))

(defn naming-convention
  [comps]
  (str (reduce #(str %1 " - " %2) comps) ".filetype"))

(defn failure
  [{total :total errors :errors :as state} message]
  (merge state {:total  (inc total)
                :errors (conj errors message)}))

(defn process-audio
  "Confirms the given file is a compatable file with the proper naming 
   convention (printing error message if not); if the process succeeds then it 
   sets the audio tag of the given file and returns the given state with both 
   :tagged and :total incremented, otherwise only increments the latter and 
   appends an error message to :errors.

   If '-v' or '-t' present in :opts then it prints the file's information.
   
   If '-t' present in :opts then it does not set any audio tags.

   If '-r' present in :opts and the given file is a directory then it runs  
   itself on the given directory, otherwise it returns the given state."
  [{opts :opts comps :comps :as state} file]
  (let [path (str/replace (. file getPath)
                          (str (. (state :source) getPath) 
                               java.io.File/separator) 
                          "")
        name (.getName file)
        type (-> name (str/split #"\.") last)
        vals (-> name (str/replace (str "." type) "") 
                 (str/split (state :splitter)))]
    (cond
      (.isDirectory file)
      (if-not (some #{"-r"} opts) state
              (reduce process-audio state (. file listFiles)))
      (not (some #{type} supported-types))
      (failure state (str "ERROR: Invalid filetype at " path "\n"))
      (not= (count vals) (count comps))
      (failure state (str "ERROR: Invalid naming convention at " path
                          "\n  Usage: " (naming-convention comps) "\n"))
      :else
      (do (when-not (some #{"-t"} opts)
            (set-tag file vals comps))
          (when (some #{"-t" "-v"} opts)
            (print-tag file path comps))
          (merge state {:tagged (inc (state :tagged))
                        :total  (inc (state :total))})))))
