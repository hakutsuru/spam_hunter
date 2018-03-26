(defproject batch_process "0.0.88"
  :description "process data generate by clojure.spec"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.9.0"]]
  :main ^:skip-aot batch_process.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
