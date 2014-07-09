(ns clj-kba.english-forum-queries
  "Generate queries for the indri index"
  (:require [clj-kba.core :as core]
            [clj-kba.english-forum-hosts :as englisher]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [edu.stanford.nlp.process PTBTokenizer DocumentPreprocessor CoreLabelTokenFactory]
           [edu.stanford.nlp.ling CoreLabel HasWord]))

(defn string-tokens
  [a-string]
  (let [ptbt (-> a-string
                 (java.io.StringReader.)
                 (PTBTokenizer.))]
    (take-while
     identity
     (repeatedly (fn []
                   (if (.hasNext ptbt)
                     (.next ptbt)
                     nil))))))

(defn string-tokens-no-punct
  [a-string]
  (filter
   (fn [x]
     (not
      (re-find #"\p{Punct}" x)))
   (string-tokens a-string)))

(defn cleaned-english-bodies
  "Retrieves cleaned english bodies from a file"
  [a-file]
  (let [collapse-html-tags (fn [a-string]
                             (string/replace a-string
                                             #"<.*>"
                                             " "))
        ;; is this even necessary
        ;; can see perf so let us do it anyway
        tokenize-and-lowercase (fn [a-string]
                                 (string/join
                                  " "
                                  (map
                                   string/lower-case
                                   (string-tokens-no-punct a-string))))]
   (map
    (fn [{body :body}]
      (-> body
          collapse-html-tags
          tokenize-and-lowercase))
    (englisher/english-only-non-4chan-forum-items a-file))))

(defn cleaned-english-bodies-directory
  [a-directory file-to-dump]
  (doseq [social-files (core/corpus-social-files a-directory)]
    (let [wrtr (io/writer file-to-dump :append true)]
      (fn [a-file]
        (let [bodies (cleaned-english-bodies a-file)]
          (doseq [body bodies]
            (binding [*out* wrtr]
              (println body)
              (flush))))))))

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
