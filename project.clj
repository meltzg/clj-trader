(defproject clj-trader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [;; CLJ Dependencies
                 [org.clojure/clojure "1.11.1"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]

                 ;; CLJS Dependencies
                 [org.clojure/clojurescript "1.11.60"]
                 [rum "0.12.11"]
                 [jayq "2.5.5"]
                 [sablono "0.8.6"]
                 [cheshire "5.8.1"]
                 [cljs-http "0.1.46"]]
  :main ^:skip-aot clj-trader.core
  :target-path "target/%s"
  :source-paths ["src/clj"
                 "src/cljs"
                 "test/cljs"]

  :aliases {"fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-bo" "dev"]}

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies   [[com.bhauman/figwheel-main "0.2.18"]
                                        [org.slf4j/slf4j-nop "2.0.7"]
                                        [com.bhauman/rebel-readline-cljs "0.1.4"]]

                       :resource-paths ["target"]
                       ;; need to add the compiled assets to the :clean-targets
                       :clean-targets  ^{:protect false} ["target"]}})
