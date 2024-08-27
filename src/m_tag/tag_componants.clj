(ns m-tag.tag-componants
  (:gen-class)
  (:import (org.jaudiotagger.tag FieldKey)))

;; Additional fields can be found in the JAudioTagger javadoc.
(def tag-map
  "Defines a map of tag fields and their corresponding field info, which is a
   map that contains the JAudioTagger FieldKey object in :field-key and the
   string template for printing in :str-template"
  {"album"        {:field-key FieldKey/ALBUM        :str-template "%-30s"}
   "album-artist" {:field-key FieldKey/ALBUM_ARTIST :str-template "%-50s"}
   "artist"       {:field-key FieldKey/ARTIST       :str-template "%-50s"}
   "disc"         {:field-key FieldKey/DISC_NO      :str-template "%-3s"}
   "num-discs"    {:field-key FieldKey/DISC_TOTAL   :str-template "%-3s"}
   "title"        {:field-key FieldKey/TITLE        :str-template "%-50s"}
   "track"        {:field-key FieldKey/TRACK        :str-template "%-3s"}
   "num-tracks"   {:field-key FieldKey/TRACK_TOTAL  :str-template "%-3s"}
   "year"         {:field-key FieldKey/YEAR         :str-template "%-5s"}})
