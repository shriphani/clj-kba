(ns clj-kba.english-forum-hosts
  "Get a list of hostnames in KBA 2012 that contain
   english posts"
  (:gen-class :main true)
  (:require [clj-kba.core :as core]
            [clojure.java.io :as io]
            [clojure.set :as c-set]
            [org.bovinegenius [exploding-fish :as uri]]))

(defn english-only-non-4chan-forum-items
  [a-file]
  (filter
   (fn [item]
     (-> item :meta :feed_language (= "English")))
   (try (core/non-4chan-forum-items a-file)
        (catch Exception e (do (println "Fuck up file " a-file)
                               [])))))

(defn english-only-forums-list
  [a-file]
  (reduce
   (fn [acc item]
     (c-set/union acc (set [(-> item :meta :home_link uri/host)])))
   (set [])
   (english-only-non-4chan-forum-items a-file)))

(defn english-only-forums-list-directory
  [a-directory]
  (reduce
   (fn [acc a-file]
     (c-set/union acc (english-only-forums-list a-file)))
   (set [])
   (core/corpus-social-files a-directory)))

(defn dump-english-only-forums
  ([a-directory]
     (dump-english-only-forums a-directory ""))
  ([a-directory prefix]
     (with-open [wrtr (io/writer (str prefix ".en.hosts") :append true)]
       (doall
        (doseq [hostname (english-only-forums-list-directory a-directory)]
          (binding [*out* wrtr]
           (println hostname)))))))

(defn main-from-repl
  "a-directory-list is a text file containing directory names"
  [a-directory-list]
  (let [dirs (clojure.string/split-lines
              (slurp a-directory-list))]
    (pmap
     (fn [a-few-dirs]
       (let [prefix (last
                     (clojure.string/split (first a-few-dirs)
                                           #"/"))]
        (doseq [a-dir a-few-dirs]
          (dump-english-only-forums a-dir
                                    prefix))))
     (partition 100 dirs))))

(defn -main
  [& args]
  ;; use the first dir as a prefix
  (apply dump-english-only-forums (concat args [(last (clojure.string/split (first args) #"/"))])))
