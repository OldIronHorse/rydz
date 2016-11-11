(defproject rydz "0.1.0-SNAPSHOT"
  :description "Private Hire booking interfaces"
  :url "https://github.com/OldIronHorse/rydz"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring.middleware.logger "0.5.0"]
                 [org.clojure/tools.logging "0.2.4"]
                 [compojure "1.5.0"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler rydz.rest/app
         :nrepl {:start? true
                 :port 999*}}
  :profiles
    {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                          [org.clojure/data.json "0.2.6"]
                          [ring-mock "0.1.5"]]}})
