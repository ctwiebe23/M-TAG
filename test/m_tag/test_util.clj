(ns m-tag.test-util
  (:import  (org.jaudiotagger.audio AudioFileIO)
            (org.jaudiotagger.tag   FieldKey))
  (:require [m-tag.core      :as core]
            [m-tag.util      :as util]
            [clojure.java.io :as io]
            [clojure.string  :as str]))

(def path
  "resources/test_files/")

(defn clear-tag
  "If the given file is a directory it runs itself on each file within the 
   directory; if the given file is a supported audio file it clears the tag of
   the file."
  [file]
  (let [type (-> file .getName (str/split #"\.") last)]
    (cond
      (.isDirectory file)
      (run! clear-tag (. file listFiles))
      (some #{type} util/supported-types)
      (let [audio (AudioFileIO/read file)
            tag   (. audio getTagOrCreateAndSetDefault)]
        (doseq [componant (seq util/componant-map)]
          (. tag (deleteField (second componant))))
        (AudioFileIO/write audio)))))

(defn clear-tags
  "Test fixture that clears the audio tags of audio files in the resources
   directory"
  [tests]
  (clear-tag (io/file path))
  (tests))