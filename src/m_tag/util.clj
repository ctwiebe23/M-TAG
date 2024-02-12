(ns m-tag.util
  (:require [clojure.string :as str])
  (:import  (org.jaudiotagger.audio AudioFileIO)
            (org.jaudiotagger.tag   FieldKey)))

(defrecord Componant
  [field-key str-size])

;; Additional fields can be found in the JAudioTagger javadoc.
(def componant-map
  {"album"        (Componant. FieldKey/ALBUM        "%-30s")
   "album_artist" (Componant. FieldKey/ALBUM_ARTIST "%-50s")
   "artist"       (Componant. FieldKey/ARTIST       "%-50s")
   "disc_number"  (Componant. FieldKey/DISC_NO      "%-3s")
   "total_discs"  (Componant. FieldKey/DISC_TOTAL   "%-3s")
   "title"        (Componant. FieldKey/TITLE        "%-50s")
   "track"        (Componant. FieldKey/TRACK        "%-3s")
   "total_tracks" (Componant. FieldKey/TRACK_TOTAL  "%-3s")
   "year"         (Componant. FieldKey/YEAR         "%-5s")})

(def supported-types
  ["mp3" "wav" "ogg" "flac"])

(defn naming-convention
  [{comps :comps}]
  (str (str/join " - " (map str/capitalize comps)) ".filetype"))

(defn print-tag
  "Prints the filepath of the given file relative to the source filepath, and
   then prints the current values of the relevant portions of the file's audio
   tag."
  [file comps]
  (let [audio (AudioFileIO/read file)]
    (run! #(printf (str "%s: " (:str-size (componant-map %)))
                   (str/capitalize %)
                   (.. audio
                       getTagOrCreateDefault
                       (getFirst (:field-key (componant-map %)))))
          comps)
    (println)))

(defn set-tag
  "Sets the tag of the given audio file according to the given vals and the
   list of componants defined in tag-format."
  [file vals comps]
  (let [audio (AudioFileIO/read file)]
    (loop [i 0]
      (when (< i (count comps))
        (.. audio
            getTagOrCreateAndSetDefault
            (setField (:field-key (componant-map (nth comps i)))
                      (->> (vals i)
                           vector
                           (into-array String))))
        (recur (inc i))))
    (AudioFileIO/write audio)))

(defn get-file-info
  "Returns a map containing the given file's :path, :type, and :vals."
  [{source :source splitter :splitter} file]
  (let [name (.getName file)
        type (last (str/split name #"\."))]
    {:path (str/replace (. file getPath)
                        (-> source
                            (. getPath)
                            (str java.io.File/separator))
                        "")
     :type type
     :vals (-> name
               (str/replace (str "." type) "")
               (str/split splitter))}))

(defn failure
  [{total :total errors :errors :as state} message]
  (merge state {:total  (inc total)
                :errors (conj errors message)}))

(defn old-process-audio
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
  (let [info (get-file-info state file)]
    (cond
      (.isDirectory file)
      (if-not (some #{"-r"} opts) state
              (reduce old-process-audio state (. file listFiles)))
      (not (some #{(info :type)} supported-types))
      (failure state (str "ERROR: Invalid filetype at " (info :path) "\n"))
      (not= (count (info :vals)) (count comps))
      (failure state (str "ERROR: Invalid naming convention at " (info :path)
                          "\n  Usage: " (naming-convention state) "\n"))
      :else
      (do (when-not (some #{"-t"} opts)
            (set-tag file (info :vals) comps))
          (when (some #{"-t" "-v"} opts)
            (print-tag file comps))
          (merge state {:tagged (inc (state :tagged))
                        :total  (inc (state :total))})))))

(defmulti process-audio
  (fn [_ file]
    (cond
      (.isDirectory file)
      :directory
      (some #{(-> file
                  .getName
                  (str/split #"\.")
                  last)}
            supported-types)
      :supported)))

(defmethod process-audio :directory
  [{opts :opts :as state} file]
  (if-not (some #{"-r"} opts) state
          (reduce process-audio state (. file listFiles))))

(defmethod process-audio :supported
  [{opts :opts comps :comps :as state} file]
  (let [info (get-file-info state file)]
    (if-not (= (count (info :vals)) (count comps))
      (failure state (str "ERROR: Invalid naming convention at " (info :path)
                          "\n  Usage: " (naming-convention state) "\n"))
      (do (when-not (some #{"-t"} opts)
            (set-tag file (info :vals) comps))
          (when (some #{"-t" "-v"} opts)
            (print-tag file comps))
          (merge state {:tagged (inc (state :tagged))
                        :total  (inc (state :total))})))))

(defmethod process-audio :default
  [state file]
  (let [info (get-file-info state file)]
    (failure state (str "ERROR: Invalid filetype at " (info :path) "\n"))))
