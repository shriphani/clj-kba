(ns clj-kba.core
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:use [byte-streams]
        [cheshire.core]
        [clojure.pprint :only [pprint]]
        [clojure.walk :only [keywordize-keys]])
  (:import [java.io BufferedInputStream File FileInputStream]
           [java.nio.charset Charset]
           [java.util.zip GZIPInputStream]
           [kba ContentItem CorpusItem StreamItem]
           [org.apache.thrift TException]
           [org.apache.thrift.transport
            TTransport
            TIOStreamTransport
            TTransportException]
           [org.apache.thrift.protocol TProtocol TBinaryProtocol TJSONProtocol]))

(defn corpus-files
  [corpus-dir]
  (println :dir-name corpus-dir)
  (flush)
  (filter
   #(re-find #".gz$" (.getAbsolutePath %))
   (file-seq (File. corpus-dir))))

(defn corpus-social-files
  [corpus-dir]
  (let [files (corpus-files corpus-dir)]
    (filter
     #(re-find #"social\."(.getAbsolutePath %))
     files)))

(defn stream-item-obj->map
  "A stream item is equipped with bytes which when
   read are lost. We fix that by building a map"
  [a-stream-item]
  (let [body (convert (.body a-stream-item)
                      ContentItem)
        source-meta-data (-> (.source_metadata a-stream-item)
                             (convert String)
                             parse-string
                             keywordize-keys)]
    {:meta source-meta-data
     :body (.raw body)}))

(defn stream-items-seq
  [a-protocol]
  (take-while
   identity
   (repeatedly
    (fn []
      (let [a-stream-item (StreamItem.)]
        (do (try (do (.read a-stream-item a-protocol)
                     (stream-item-obj->map a-stream-item))
                 (catch TTransportException e nil))))))))

(defn read-file
  "Reads the KBA streamcorpus file
   builds a metadata and body map"
  [a-file]
  (let [transport (-> a-file
                      (FileInputStream.)
                      (GZIPInputStream.)
                      (TIOStreamTransport.))
        
        protocol  (TBinaryProtocol. transport)]
    (do (.open transport)
        (stream-items-seq protocol))))

(defn forum-items
  [a-file]
  (let [items (read-file a-file)]
    (filter
     (fn [item]
       (= "Forum"
          (-> item
              :meta
              :feed_class)))
     items)))

(defn non-4chan-forum-items
  [a-file]
  (let [items (forum-items a-file)]
    (filter
     (fn [item]
       (not
        (re-find #"4chan"
                 (-> item
                     :meta
                     :home_link))))
     items)))

(defn most-recent-body-host-name
  [a-file]
  (let [cset (Charset/forName "UTF-8")]
   (reduce
    (fn [acc item]
      (merge acc {(-> item :meta :home_link)
                  (:body item)}))
    {}
    (non-4chan-forum-items a-file))))

(defn corpus-most-recent-body-host-name
  [corpus-dir]
  (reduce
   (fn [acc file]
     (do (println :processing (.getAbsolutePath file))
         (flush)
         (let [to-merge (try (most-recent-body-host-name file)
                             (catch Exception e (println
                                                 (str "Fuck up file "
                                                      (.getAbsolutePath file)))))]
           (merge acc to-merge))))
   {}
   (corpus-social-files corpus-dir)))

(defn dump-host-names-bodies
  ([corpus-dir]
     (dump-host-names-bodies corpus-dir ""))
  ([corpus-dir prefix]
     (let [to-dump-file (str prefix "-host-name-latest-body.clj")]
       (println :dumping-to to-dump-file)
       (with-open [wrtr (io/writer to-dump-file)]
         (pprint (corpus-most-recent-body-host-name corpus-dir)
                 wrtr)))))

(defn -main
  [& args]
  (let [arguments (:arguments (parse-opts args []))]
    (apply dump-host-names-bodies arguments)))
