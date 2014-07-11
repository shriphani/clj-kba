(ns clj-kba.english-forum-queries
  "Generate queries for the indri index"
  (:gen-class :main true)
  (:require [byte-streams :refer :all]
            [clj-kba.core :as core]
            [clj-kba.english-forum-hosts :as englisher]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [edu.stanford.nlp.process PTBTokenizer DocumentPreprocessor CoreLabelTokenFactory]
           [edu.stanford.nlp.ling CoreLabel HasWord]))

(defn string-tokens
  [a-string]
  (let [ptbt (-> a-string
                 (java.io.StringReader.)
                 (PTBTokenizer. (CoreLabelTokenFactory.) ""))]
    (take-while
     identity
     (repeatedly (fn []
                   (if (.hasNext ptbt)
                     (-> ptbt (.next) (.toString))
                     nil))))))

(defn string-tokens-no-punct
  [a-string]
  (filter
   (fn [x]
     (not
      (try (re-find #"\p{Punct}" x)
           (catch Exception e nil))))
   (string-tokens a-string)))

(defn try-to-convert
  [bytes result-type filename]
  "This is so we skip over the offending segments
   in the KBA corpus. log the buggy types for later maybe"
  (try (convert bytes result-type)
       (catch Exception e (do (println :problem-reading-bytes filename)
                              nil))))

(defn cleaned-english-bodies
  "Retrieves cleaned english bodies from a file"
  [a-file]
  (let [collapse-html-tags (fn [a-string]
                             (if-not (nil? a-string)
                               (string/replace a-string
                                               #"<.*>"
                                               " ")
                               nil))
        ;; is this even necessary
        ;; can see perf so let us do it anyway
        tokenize-and-lowercase (fn [a-string]
                                 (if-not (nil? a-string)
                                   (string/join
                                    " "
                                    (map
                                     string/lower-case
                                     (string-tokens-no-punct a-string)))
                                   nil))]
    (filter
     identity
     (map
      (fn [{body :body}]
        (-> body
            (try-to-convert String a-file)
            collapse-html-tags
            tokenize-and-lowercase))
      (englisher/english-only-non-4chan-forum-items a-file)))))

(defn cleaned-english-bodies-directory
  [a-directory file-to-dump]
  (doseq [a-file (core/corpus-social-files a-directory)]
    (let [wrtr (io/writer file-to-dump :append true)
          bodies (cleaned-english-bodies a-file)]
      (doseq [body bodies]
        (binding [*out* wrtr]
          (println body)
          (flush)))
      (.close wrtr))))

(defn cleaned-english-bodies-corpus
  "Supply a text file containing a list
   of KBA directories to work with"
  [a-directory-list-file]
  (let [directories (string/split-lines
                     (slurp a-directory-list-file))]
    (doseq [directory directories]
      (cleaned-english-bodies-directory directory (str a-directory-list-file
                                                       ".queries")))))

(defn -main
  [& args]
  (cleaned-english-bodies-corpus (first args)))
