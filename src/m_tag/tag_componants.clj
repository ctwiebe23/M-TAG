(ns m-tag.tag-componants
  (:gen-class)
  (:import (org.jaudiotagger.tag FieldKey)))

;; Additional fields can be found in the JAudioTagger javadoc.
(def tag-map
  {"album"        {:field-key FieldKey/ALBUM        :str-size "%-30s"}
   "album-artist" {:field-key FieldKey/ALBUM_ARTIST :str-size "%-50s"}
   "artist"       {:field-key FieldKey/ARTIST       :str-size "%-50s"}
   "disc"         {:field-key FieldKey/DISC_NO      :str-size "%-3s"}
   "num-discs"    {:field-key FieldKey/DISC_TOTAL   :str-size "%-3s"}
   "title"        {:field-key FieldKey/TITLE        :str-size "%-50s"}
   "track"        {:field-key FieldKey/TRACK        :str-size "%-3s"}
   "num-tracks"   {:field-key FieldKey/TRACK_TOTAL  :str-size "%-3s"}
   "year"         {:field-key FieldKey/YEAR         :str-size "%-5s"}})
