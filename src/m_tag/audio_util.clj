(ns m-tag.audio-util
  (:gen-class)
  (:require [clojure.string       :as str]
            [m-tag.tag-componants :as tag]
            [m-tag.program-state  :as state])
  (:import  (org.jaudiotagger.audio AudioFileIO)))

(def supported-types
  "A vector of audio types support by M'TAG."
  ["mp3" "wav" "ogg" "flac"])

(defn naming-convention
  "Returns a string containing the proper naming convention given the program
   state."
  [{fields :fields}]
  (str (->> fields
            (map str/capitalize)
            (str/join " - "))
       ".filetype"))

(defn field->str
  "Returns a string representing the given field as found in the given audio
   file."
  [audio field]
  (format (str "%s: " (get-in tag/tag-map [field :str-template]))
          field
          (.. audio
              getTagOrCreateDefault
              (getFirst (get-in tag/tag-map [field :field-key])))))

(defn print-tag
  "Prints the values of the given tag fields in the given file."
  [file fields]
  (let [audio     (AudioFileIO/read file)
        print-all (comp println str/trim (partial str/join ""))]
    (print-all (map #(field->str audio %) fields))))

(defn set-tag
  "Sets the tag of the given audio file according to the given values and the
   given fields."
  [file fvals fields]
  (let [audio      (AudioFileIO/read file)
        java-array (comp (partial into-array String) vector)]
    (doseq [[field fval] (map vector fields fvals)]
      (.. audio
          getTagOrCreateAndSetDefault
          (setField (get-in tag/tag-map [field :field-key])
                    (java-array fval))))
    (AudioFileIO/write audio)))

(defn clear-tag
  "Clears the audio tag of the given file."
  [file]
  (let [audio (AudioFileIO/read file)
        tag   (. audio getTagOrCreateAndSetDefault)]
    (doseq [field (vals tag/tag-map)]
      (. tag (deleteField (field :field-key))))
    (AudioFileIO/write audio)))

(defn file->file-info
  "Returns a map containing the :file, :path, :type, and :fvals of the given
   file."
  [{source :source splitter :splitter} file]
  (let [name      (.getName file)
        file-type (last (str/split name #"\."))]
    {:file  file
     :path  (str/replace (. file getPath)
                         (-> source
                             (. getPath)
                             (str java.io.File/separator))
                         "")
     :type  (cond
              (.isDirectory file)
              :directory
              (some #{file-type} supported-types)
              file-type
              :else
              :unsupported)
     :fvals (-> name
                (str/replace (str "." file-type) "")
                (str/split splitter))}))

(defmulti process-audio
  "Takes a program state and a file info and returns a program state.
   
   Processes the given file differently depending on whether it's a :directory,
   a :supported file, or something else.
   
   Directories will be either ignored or reduced over depending on whether the
   recursive option is set.
   
   Supported files will have their tags set, cleared, printed, or some
   combination thereof depending on different opts.
   
   Other files will be recorded as error messages."
  (fn [_ {type :type}]
    type))

(defmethod process-audio :directory
  [{opts :opts :as state} {file :file}]
  (if-not (some #{"-r"} opts)
    state
    (let [files      (. file listFiles)
          file-infos (map #(file->file-info state %) files)]
      (reduce process-audio state file-infos))))

(defmethod process-audio :unsupported
  [state {path :path}]
  (state/failure state (str "ERROR: Invalid filetype at " path "\n")))

(defmethod process-audio :default
  [{opts :opts fields :fields :as state} {file :file fvals :fvals path :path}]
  (if-not (= (count fvals) (count fields))
    (state/failure state
                   (str "ERROR: Invalid naming convention at " path
                        "\n  Usage: " (naming-convention state) "\n"))
    (do (when-not (some #{"-t"} opts)
          (if (some #{"-c"} opts)
            (clear-tag file)
            (set-tag file fvals fields)))
        (when (some #{"-t" "-v"} opts)
          (print-tag file fields))
        (merge state {:tagged (inc (state :tagged))
                      :total  (inc (state :total))}))))
