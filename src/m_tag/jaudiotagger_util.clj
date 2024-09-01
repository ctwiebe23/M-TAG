(ns m-tag.jaudiotagger-util
  (:gen-class)
  (:import (org.jaudiotagger.tag TagOptionSingleton)
           (java.util.logging    Logger
                                 Level)))

(defn configure-jat
  "Configure JAudioTagger so that it shrinks excess space allocated to tags if
   such space is present; also silences the JAudioTagger logger unless '-j' is
   present in :opts."
  [{opts :opts}]
  (.. TagOptionSingleton
      getInstance
      (setId3v2PaddingWillShorten true))
  (when-not (some #{"-j"} opts)
    (.. Logger
        (getLogger "org.jaudiotagger")
        (setLevel (. Level OFF)))))
