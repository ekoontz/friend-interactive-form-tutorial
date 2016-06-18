(defproject friend-form-login "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.1.3"]
                 [com.cemerick/friend "0.2.3"]
                 [environ "1.0.0"]
                 [clj-http "2.2.0"]
                 [http-kit "2.1.16"]
                 [friend-oauth2 "0.1.3"]
                 [org.clojure/data.json "0.2.5"] 
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-core "1.3.2"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler friend-form-login.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})

