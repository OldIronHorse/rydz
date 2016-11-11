(ns rydz.rest
  (:use compojure.core
    ring.middleware.json)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [response]]
            [ring.middleware.logger :as logger]
            [clojure.tools.logging :as log]
            [rydz.core refer :all]))

(defroutes app-routes
  (route/not-found (response {:message "Page not found"})))
