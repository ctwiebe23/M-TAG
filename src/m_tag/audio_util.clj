(ns m-tag.audio-util
  (:gen-class)
  (:require [clojure.string       :as str]
            [m-tag.tag-componants :as tag]
            [m-tag.program-state  :as state])
  (:import  (org.jaudiotagger.audio AudioFileIO)))

(def supported-types
  ["mp3" "wav" "ogg" "flac"])

(defn naming-convention
  [{fields :fields}]
  (str (->> fields
            (map str/capitalize)
            (str/join " - "))
       ".filetype"))

(defn print-tag
  "Prints the filepath of the given file relative to the source filepath, and
   then prints the current values of the relevant portions of the file's audio
   tag."
  [file fields]
  (let [audio     (AudioFileIO/read file)
        print-all (comp println str/trim (partial str/join ""))]
    (print-all (map #(format (str "%s: " (get-in tag/tag-map [% :str-size]))
                             %
                             (.. audio
                                 getTagOrCreateDefault
                                 (getFirst (get-in tag/tag-map
                                                   [% :field-key]))))
                    fields))))

(defn set-tag
  "Sets the tag of the given audio file according to the given vals and the
   list of componants defined in tag-format."
  [file vals fields]
  (let [audio      (AudioFileIO/read file)
        pairs      (map vector fields vals)
        java-array (comp (partial into-array String) vector)]
    (doseq [pair pairs]
      (.. audio
          getTagOrCreateAndSetDefault
          (setField (get-in tag/tag-map [(first pair) :field-key])
                    (java-array (second pair)))))
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

(defn clear-tag
  [file]
  (let [audio (AudioFileIO/read file)
        tag   (. audio getTagOrCreateAndSetDefault)]
    (doseq [field (vals tag/tag-map)]
      (. tag (deleteField (:field-key field))))
    (AudioFileIO/write audio)))

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
  [{opts :opts fields :fields :as state} file]
  (let [info (get-file-info state file)]
    (if-not (= (count (info :vals)) (count fields))
      (state/failure state
                     (str "ERROR: Invalid naming convention at " (info :path)
                          "\n  Usage: " (naming-convention state) "\n"))
      (do (when-not (some #{"-t"} opts)
            (if (some #{"-c"} opts) (clear-tag file)
                (set-tag file (info :vals) fields)))
          (when (some #{"-t" "-v"} opts)
            (print-tag file fields))
          (merge state {:tagged (inc (state :tagged))
                        :total  (inc (state :total))})))))

(defmethod process-audio :default
  [state file]
  (let [info (get-file-info state file)]
    (state/failure state
                   (str "ERROR: Invalid filetype at " (info :path) "\n"))))
