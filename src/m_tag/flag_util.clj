(ns m-tag.flag-util
  (:gen-class))

(defprotocol Printable
  "Object can be represented by a string"
  (to-str
    [_]
    "Returns a string representing the subject record"))

(defprotocol Flag-Validator
  "Protocol for the Flag-Engine record"
  (get-pairs
    [_ args]
    "Returns `nil` and prints an error when given args that violate the
    engine's `opts-map`, returns `nil` and prints valid flags when `-h` or
    `--help` is present in the given args, and returns a vector of maps that
    pair each flag with its args (in order of their appearance) on a success")
  (get-lists
    [_ args]
    "Returns `nil` and prints an error when given args that violate the
    engine's `opts-map`, returns `nil` and prints valid flags when `-h` or
    `--help` is present in the given args, and returns a map that contains 2
    vectors (1 contains the flags found in args in order of their appearance,
    the other contains the arguments in order of their appearance"))

(defn check-flags
  "Takes a string starting with '-' that is parsed as a collection of flags.
  If a flag is present that is not found in opts-map, an error is thrown and
  nil is returned; otherwise a vector of characters containing each flag (and
  duplicate flags) in order of appearance is returned."
  [flags opts-map]
  (loop [f (rest flags)
         o []]
    (if (empty? f)
      o
      (let [focus (first f)
            tail  (rest f)]
        (if (opts-map focus)
          (recur tail (conj o focus))
          (println "ERROR:" focus "not a valid flag"))))))

(defrecord Flag
           [desc names]
  Printable
  (to-str
    [_]
    (str (reduce str (for [n names]
                       (str " --" n)))
         "\n   "
         desc)))

(defrecord Flag-Engine
           [opts-map]
  Flag-Validator
  Printable

  (get-pairs
    [_ args]
    (loop [a args
           m []]
      (if (empty? a)
        m
        (let [focus (first a)
              tail  (rest a)]
          (if (= \- (get focus 0))
            (let [new-flags (check-flags focus opts-map)]
              (when new-flags
                (recur tail
                       (apply conj m (for [f new-flags]
                                       {:flag    f
                                        :strings []})))))
            (recur tail
                   (reduce (fn [switch coll]
                             (if-not (vector? switch)
                               (vector (merge coll
                                              {:strings (conj (:strings coll)
                                                              switch)}))
                               (apply vector coll switch)))
                           focus
                           (reverse m))))))))

  (get-lists
    [_ args]
    (loop [a args
           f []
           s []]
      (if (empty? a)
        {:flags   (distinct f)
         :strings s}
        (let [focus (first a)
              tail  (rest a)]
          (if (= \- (get focus 0))
            (let [new-flags (check-flags focus opts-map)]
              (when new-flags
                (recur tail
                       (apply conj f new-flags)
                       s)))
            (recur tail
                   f
                   (conj s focus)))))))

  (to-str
    [_]
    (reduce str (for [pair (seq opts-map)]
                  (str "\n-" (pair 0) (.to-str (pair 1)) "\n")))))
