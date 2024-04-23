(ns m-tag.audio-record
  (:require [clojure.string  :as str]
            [m-tag.constants :refer [supported-types]]))

;; A record of an audio file.
(defrecord Audio-Record
           [file path type vals])

(defn get-path
  "Returns the path to the file relative to the given source"
  [file source]
  (str/replace (. file getPath)
               (-> source
                   (. getPath)
                   (str java.io.File/separator))
               ""))

(defn file->Record
  "Converts the given file to a Audio Record"
  [{source :source splitter :splitter} file]
  (let [name (.getName file)
        type (last (str/split name #"\."))]
    (map->Audio-Record {:file file
                        :path (get-path file source)
                        :type (cond
                                (.isDirectory file)
                                :directory
                                (not (some #{type} supported-types))
                                :unsupported
                                :else
                                type)
                        :vals (-> name
                                  (str/replace (str "." type) "")
                                  (str/split splitter))})))
