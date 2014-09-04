(ns clj-kba.uri-index-names
  "Retrieve index page names and landing uris from stuff"
  (:require [clj-kba.core :as core]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:use [clojure.pprint :only [pprint]]))

(defn uris-index-names
  [a-file]
  (doseq [item (core/non-4chan-forum-items a-file)]
    (when (-> item :meta :language (= "English"))
      (pprint {:uri (-> item :meta :home_link)
               :index-name (-> item :meta :item_categories)}))))

(defn process-batch
  [kba-files-list]
  (let [kba-dirs (string/split-lines
                  (slurp kba-files-list))]
    (doseq [dir kba-dirs]
      (doseq [f (core/corpus-social-files dir)]
        (uris-index-names f)))))

(defn -main
  [& args]
  (let [filename (first args)
        out-file (str filename ".index-page-names.clj")
        out-handle (io/writer out-file :append true)]
    (binding [*out* out-handle]
      (process-batch filename))))
