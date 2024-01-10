(ns m-tag.core-test
  (:require [m-tag.core      :refer [validate-args]]
            [m-tag.util      :refer [set-tag
                                     process-audio]]
            [m-tag.test-util :refer [path
                                     clear-tags]]
            [clojure.test    :refer [deftest
                                     testing
                                     is
                                     use-fixtures]]))

(use-fixtures :each clear-tags)

(deftest set-tag-tests)

(deftest process-audio-tests)

(deftest validate-args-tests
  (is (not (validate-args [])))
  (testing "filepath"
    (is (validate-args [path]))
    (is (not (validate-args ["bad/file/path/"])))
    (is (not (validate-args [path path]))))
  (testing "options"
    (is (validate-args [path "-r" "-t" "-v" "-s"]))
    (is (validate-args [path "-s" "-t" "-v" "-r"]))
    (is (validate-args [path "-v"]))
    (is (validate-args [path "-r" "-r" "-s"]))
    (is (not (validate-args [path "-b"])))
    (is (not (validate-args [path "-r-r"])))
    (is (not (validate-args [path "-r" "-b" "-t"]))))
  (testing "format"))