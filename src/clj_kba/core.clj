(ns clj-kba.core
  (:import [java.io BufferedInputStream File FileInputStream]
           [java.util.zip GZIPInputStream]
           [kba StreamItem StreamTime CorpusItem]
           [org.apache.thrift TException]
           [org.apache.thrift.transport
            TTransport
            TIOStreamTransport
            TTransportException]
           [org.apache.thrift.protocol TProtocol TBinaryProtocol]))

(defn corpus-files
  [corpus-dir]
  (filter
   #(re-find #".gz$" (.getAbsolutePath %))
   (file-seq (File. corpus-dir))))

(defn corpus-social-files
  [corpus-dir]
  (let [files (corpus-files corpus-dir)]
    (filter
     #(re-find #"social\."(.getAbsolutePath %))
     files)))

(defn stream-items-seq
  [a-protocol]
  (take-while
   identity
   (repeatedly
    (fn []
      (let [a-stream-item (StreamItem.)]
        (do (try (do (.read a-stream-item a-protocol)
                     (println a-stream-item)
                     a-stream-item)
                 (catch TTransportException e nil))))))))

(defn read-file
  [a-file]
  (let [transport (-> a-file
                      (FileInputStream.)
                      (GZIPInputStream.)
                      (TIOStreamTransport.))

        protocol  (TBinaryProtocol. transport)]
    (do (.open transport)
        (stream-items-seq protocol))))
