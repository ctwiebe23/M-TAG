(ns m-tag.util
  (:require [clojure.string :as str])
  (:import  (org.jaudiotagger.audio AudioFileIO)
            (org.jaudiotagger.tag   FieldKey)))

;; A record of an audio file.
(defrecord Audio-Record
           [file path type vals])

;; A componant record.
(defrecord Comp
           [field-key str-size])

;; Additional fields can be found in the JAudioTagger javadoc.
(def Comp-map
  {"album"        (Comp. FieldKey/ALBUM        "%-30s")
   "album_artist" (Comp. FieldKey/ALBUM_ARTIST "%-50s")
   "artist"       (Comp. FieldKey/ARTIST       "%-50s")
   "disc_number"  (Comp. FieldKey/DISC_NO      "%-3s")
   "total_discs"  (Comp. FieldKey/DISC_TOTAL   "%-3s")
   "title"        (Comp. FieldKey/TITLE        "%-50s")
   "track"        (Comp. FieldKey/TRACK        "%-3s")
   "total_tracks" (Comp. FieldKey/TRACK_TOTAL  "%-3s")
   "year"         (Comp. FieldKey/YEAR         "%-5s")})

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
    (println
     (str/trim
      (reduce #(str %1 (format (str "%s: " (:str-size (Comp-map %2)))
                               (str/capitalize %2)
                               (.. audio
                                   getTagOrCreateDefault
                                   (getFirst (:field-key (Comp-map %2))))))
              "" comps)))))

(defn set-tag
  "Sets the tag of the given audio file according to the given vals and the
   list of componants defined in tag-format."
  [file vals comps]
  (let [audio (AudioFileIO/read file)]
    (loop [i 0]
      (when (< i (count comps))
        (.. audio
            getTagOrCreateAndSetDefault
            (setField (:field-key (Comp-map (nth comps i)))
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

(defmulti clear-tag
  "If the given file is a directory it runs itself on each file within the
   directory; if the given file is a supported audio file it clears the tag of
   the file."
  (fn [state file]
    (cond
      (.isDirectory file)
      :directory
      (some #{((get-file-info state file) :type)} supported-types)
      :supported)))

(defmethod clear-tag :directory
  [{opts :opts} file]
  (when (some #{"-r"} opts)
    (run! clear-tag (. file listFiles))))

(defmethod clear-tag :supported
  [_ file]
  (let [audio (AudioFileIO/read file)
        tag   (. audio getTagOrCreateAndSetDefault)]
    (doseq [componant (seq Comp-map)]
      (. tag (deleteField (:field-key (second componant)))))
    (AudioFileIO/write audio)))

(defn failure
  [{total :total errors :errors :as state} message]
  (merge state {:total  (inc total)
                :errors (conj errors message)}))

(defmulti process-audio
  "Confirms the given file is a compatable file with the proper naming
   convention (printing error message if not); if the process succeeds then it
   sets the audio tag of the given file and returns the given state with both
   :tagged and :total incremented, otherwise only increments the latter and
   appends an error message to :errors.

   If '-v' or '-t' present in :opts then it prints the file's information.

   If '-t' present in :opts then it does not set any audio tags.

   If '-r' present in :opts and the given file is a directory then it runs
   itself on the given directory, otherwise it returns the given state."
  (fn [state file]
    (cond
      (.isDirectory file)
      :directory
      (some #{((get-file-info state file) :type)} supported-types)
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
            (if (some #{"-c"} opts) (clear-tag state file)
                (set-tag file (info :vals) comps)))
          (when (some #{"-t" "-v"} opts)
            (print-tag file comps))
          (merge state {:tagged (inc (state :tagged))
                        :total  (inc (state :total))})))))

(defmethod process-audio :default
  [state file]
  (let [info (get-file-info state file)]
    (failure state (str "ERROR: Invalid filetype at " (info :path) "\n"))))
