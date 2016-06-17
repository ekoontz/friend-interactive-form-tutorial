(defproject friend-form-login "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.3"]
                 [ring/ring-core "1.2.0"]
                 [com.cemerick/friend "0.2.3"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler friend-form-login.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
