(defproject clj-kba "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[byte-streams "0.1.10"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [cheshire "5.3.1"]
                 [org.apache.thrift/libthrift "0.9.1"]
                 [org.bovinegenius/exploding-fish "0.3.4"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]]
  :java-source-paths ["src/gen-java"]
  :jvm-opts ["-Xmx10g"]
  :main clj-kba.english-forum-hosts)
